package com.dumbhippo.jive;

import javax.ejb.EJB;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;

import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.downloads.Download;
import com.dumbhippo.server.downloads.DownloadConfiguration;
import com.dumbhippo.server.downloads.DownloadPlatform;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxRunnable;
import com.dumbhippo.tx.TxUtils;

@IQHandler(namespace=ClientInfoIQHandler.CLIENT_INFO_NAMESPACE)
public class ClientInfoIQHandler extends AnnotatedIQHandler {
	static final String CLIENT_INFO_NAMESPACE = "http://dumbhippo.com/protocol/clientinfo";
	
	@EJB
	private Configuration config;

	@EJB
	private AccountSystem accountSystem;
	
	public ClientInfoIQHandler() {
		super("Dumbhippo clientInfo IQ Handler");
		Log.debug("creating ClientInfoIQHandler");
	}

	@IQMethod(name="clientInfo", type=IQ.Type.get)
	public void getClientInfo(final UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Element child = request.getChildElement();
		
		final String platformName = child.attributeValue("platform");
        if (platformName == null) {
        	throw IQException.createBadRequest("clientInfo IQ missing platform attribute");
        }

        // optional distribution/version info
        String rawDistribution = child.attributeValue("distribution");
        String rawVersion = child.attributeValue("version");

        final String distributionVersion;
        final String osVersion;
        
        // backwards compatibility
        if ("fedora5".equals(rawDistribution)) {
        	distributionVersion = "Fedora";
        	osVersion = "5";
        } else if ("fedora6".equals(rawDistribution)) {
        	distributionVersion = "Fedora";
        	osVersion = "6";
        } else {
        	distributionVersion = rawDistribution;
        	osVersion = rawVersion;
        }
        
        String rawArchitecture = child.attributeValue("architecture");
        final String architecture;
        if ("i386".equals(rawArchitecture) ||
        	"i486".equals(rawArchitecture) ||
        	"i586".equals(rawArchitecture) ||
        	"i686".equals(rawArchitecture))
        	architecture = "x86";
        else if ("powerpc".equals(rawArchitecture))
        	architecture = "ppc";
        else if ("powerpc64".equals(rawArchitecture))
        	architecture = "ppc64";
        else
        	architecture = rawArchitecture;
        		
        DownloadConfiguration downloads = config.getDownloads();
        DownloadPlatform platform;
        try {
			platform = downloads.findPlatform(platformName);
		} catch (NotFoundException e) {
			throw IQException.createBadRequest("clientInfo IQ: unrecognized platform: '" + platformName + "'");
		}
		
        Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("clientInfo", CLIENT_INFO_NAMESPACE);
		childElement.addAttribute("minimum", platform.getMinimum());
		childElement.addAttribute("current", platform.getVersion());

		try {
			Download download = downloads.findDownload(platformName, distributionVersion, osVersion, architecture);
			childElement.addAttribute("download", download.getUrl());
		} catch (NotFoundException e) {
			// We set the attribute anyway so older client versions don't throw an error.
			// (We'll always have a download for Windows; Linux doesn't use the download URL in
			// any case, though perhaps it should)
			childElement.addAttribute("download", "http://example.com/notused");
		}
		
		reply.setChildElement(childElement);
		
		TxUtils.runInTransactionOnCommit(new TxRunnable() {
			public void run() throws RetryException {
				accountSystem.updateClientInfo(viewpoint, platformName, distributionVersion, osVersion, architecture);
			}
		});
	}
}
