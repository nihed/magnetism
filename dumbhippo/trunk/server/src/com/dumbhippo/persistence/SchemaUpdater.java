package com.dumbhippo.persistence;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Table;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.tool.hbm2ddl.ColumnMetadata;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.IndexMetadata;
import org.hibernate.tool.hbm2ddl.TableMetadata;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

/**
 * This class handles doing custom schema updates when our application starts.
 * The principal reason we have it currently is that Hibernate's built-in
 * schema updater can't handle @Index annotations. Index creation is handled
 * only when hibernate.hdm2ddl.auto=create, while we use 'update'. So we
 * handle adding indexes ourself.
 * 
 * We use Hibernate internals to handle deciphering the current state of the database.
 * 
 * The code is mildly MySQL-specific; we explicitly create a MySQLDialect rather
 * than trying to figure out the current Hibernate dialect, and the SQL statements
 * to create and drop indexes may also be MySQL specific. (The dialect object could
 * potentially be used to handle syntax differences for index maintenance)
 * 
 * One thing that isn't handled here is *removing* indices that we specified in annotations
 * before but are no longer there; this is close to impossible, since it's hard to figure
 * out what indices are leftovers and what indices are addded by Hibernate automatically. 
 * You could guess from the name that any index that isn't called PRIMARY or FK... 
 * is a leftover, but to be safe, we just leave old indexes around.
 * 
 * @author otaylor
 */
public class SchemaUpdater {
	private static final Logger logger = GlobalSetup.getLogger(SchemaUpdater.class);
	
	private DataSource dataSource;
	private Connection connection;
	private DatabaseMetadata meta;
	
	private SchemaUpdater(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	private void open() throws SQLException {
		Dialect dialect = new MySQLDialect();
		connection = dataSource.getConnection();
		meta = new DatabaseMetadata(connection, dialect);
	}
	
	private void close() throws SQLException {
		connection.close();
	}
	
	static private final String CLASS_PREFIX = "com/dumbhippo/persistence/";
	static private final String CLASS_SUFFIX = ".class";
	
	/**
	 * Retrieve a list of all classes in the enclosing package. Java doesn't
	 * have provisions for enumerating the classes in a package (The flexibility 
	 * of the ClassLoader setup makes that slightly nonsensical), so we have
	 * to dig out a reference to the .par file, list the entries in it, and
	 * reverse engineer from that to class names.
	 * 
	 * @return List of classes in the enclosing package
	 */
	public List<Class<?>> getClasses() {
		ClassLoader loader = SchemaUpdater.class.getClassLoader();
		URL resource  = loader.getResource("META-INF/persistence.xml");
		
		if (!resource.getProtocol().equals("jar"))
			throw new RuntimeException("Can't list classes for updating because they aren't loaded from a jar");
		
		List<Class<?>> classes = new ArrayList<Class<?>>();

		try {
			URLConnection connection = resource.openConnection();
			JarFile file = ((JarURLConnection)connection).getJarFile();
			
			Enumeration<JarEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (entryName.startsWith(CLASS_PREFIX) && entryName.endsWith(CLASS_SUFFIX)) {
					String className = ("com.dumbhippo.persistence." +
										entryName.substring(CLASS_PREFIX.length(),
							                                entryName.length() - CLASS_SUFFIX.length()));
					try {
						classes.add(loader.loadClass(className));
					} catch (ClassNotFoundException e) {
						logger.warn("Failed to load class {}", className);
					}
				}
			}
			
		} catch (IOException e) {
			throw new RuntimeException("Can't open jar to list classes for updating", e);
		} 
		
		return classes;
	}
	
	private boolean existingIndexMatches(IndexMetadata indexMeta, Index indexAnnotation) {
		Set<String> oldColumns = new HashSet<String>();
		Set<String> newColumns = new HashSet<String>();
		
		for (ColumnMetadata columnMeta : indexMeta.getColumns()) {
			oldColumns.add(columnMeta.getName());
		}
		for (String columnName : indexAnnotation.columnNames()) {
			newColumns.add(columnName);
		}
		if (oldColumns.size() != newColumns.size())
			return false;
		for (String column : oldColumns)
			if (!newColumns.contains(column))
				return false;
		
		return true;
	}
	
	private void dropIndex(String tableName, String indexName) throws SQLException {
		String sql = "ALTER TABLE " + tableName + " DROP INDEX " + indexName;
		logger.info("Removing old index: {}", sql);

		Statement statement = connection.createStatement();
		statement.execute(sql);
		statement.close();
	}
	
	private void createIndex(TableMetadata tableMeta, String indexName, String[] columnNames) throws SQLException {
		String tableName = tableMeta.getName();
		StringBuffer columnString = new StringBuffer();
		for (String columnName : columnNames) {
			ColumnMetadata columnMeta = tableMeta.getColumnMetadata(columnName);
			if (columnMeta == null)
				throw new RuntimeException("Unknown column " + columnName + " referenced from index.");

			if (columnString.length() > 0)
				columnString.append(",");
			
			columnString.append(columnName);
		
			// varchar(255) columns cause problems for indices, since the total
			// number of bytes indexed must be less than 1000 for MySQL, and
			// with UTF-8 encoding, varchar(255) is treated as 763 bytes. Plus
			// it's just silly to index that much in most cases.
			if (columnMeta.getTypeCode() == Types.VARCHAR)
				columnString.append("(20)");
		}
		
		String sql = "ALTER TABLE " + tableName + " ADD INDEX " + indexName + "(" + columnString + ")";
		logger.info("Adding index: {}", sql);
		
		Statement statement = connection.createStatement();
		statement.execute(sql);
		statement.close();
	}
	
	private void updateIndex(TableMetadata tableMeta, Index indexAnnotation) {
		try {
			IndexMetadata indexMeta = tableMeta.getIndexMetadata(indexAnnotation.name());
			if (indexMeta != null) {
				if (existingIndexMatches(indexMeta, indexAnnotation)) {
					logger.debug("Index {} on table {} already exists and matches", indexMeta.getName(), tableMeta.getName());
					return;
				}
				dropIndex(tableMeta.getName(), indexMeta.getName());
			}
			createIndex(tableMeta, indexAnnotation.name(), indexAnnotation.columnNames());
		} catch (SQLException e) {
			throw new RuntimeException("SQL Exception updating index", e);
		}
	}
	
	private void updateTable(Table tableAnnotation) {
		String name = tableAnnotation.appliesTo();
		TableMetadata tableMeta = meta.getTableMetadata(name , "dumbhippo", null);
		if (tableMeta == null) {
			logger.warn("Couldn't find metadata for table {}, it probably doesn't exist yet", name);
		} else {
			for (Index indexAnnotation : tableAnnotation.indexes()) {
				updateIndex(tableMeta, indexAnnotation);
			}
		}
	}
	
	private void updateTables() {
		for (Class<?> clazz : getClasses()) {
			Table tableAnnotation = clazz.getAnnotation(Table.class);
			if (tableAnnotation != null) {
				updateTable(tableAnnotation);
			}
		}
	}
	
	/**
	 * Do any custom schema updating that is necessary. This method can safely
	 * be called every time the application is started since it checks to see
	 * what updates are needed before making them.
	 */
	static public void update() {
		logger.debug("Updating schemas...");
		try {
			DataSource dataSource = (DataSource)(new InitialContext()).lookup("java:/DumbHippoDS");
			SchemaUpdater updater = new SchemaUpdater(dataSource);
			
			updater.open();
			updater.updateTables();
			updater.close();
			 
			logger.debug("Finished updating schemas");
		} catch (NamingException e) {
			throw new RuntimeException("Couldn't get DataSource to update schemas", e);
		} catch (SQLException e) {
			throw new RuntimeException("SQL exception updating schemas", e);
		}
	}
}
