%{!?python_sitelib: %define python_sitelib %(python -c "from distutils.sysconfig import get_python_lib; print get_python_lib()")}

Name:           bigboard
Version:        0.3.1
Release:        1
Summary:        Sidebar application launcher using mugshot.org

Group:          Applications/Internet
License:        GPL
URL:            http://mugshot.org/
Source0:        http://download.mugshot.org/extras/bigboard/source/bigboard-%{version}.tar.gz
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

Requires:       mugshot >= 1.1.42-1
Requires:       deskbar-applet
Requires:       hippo-canvas-python >= 0.2.16
Requires:       gnome-python2-desktop
Requires:       gnome-python2-gnomedesktop
Requires:       gnome-python2-gnomekeyring
Requires:       dbus-python

BuildRequires:	pkgconfig
BuildRequires:	gtk2-devel
BuildRequires:	pygtk2-devel
BuildRequires:	gnome-keyring-devel

%description
Bigboard is a sidebar and application launcher that works with mugshot.org
to provide an online experience.

%prep
%setup -q

%build
%configure --disable-static
make %{?_smp_mflags}

%install
rm -rf $RPM_BUILD_ROOT
make install DESTDIR=$RPM_BUILD_ROOT

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%doc COPYING
%attr(0755,root,root) %{_libexecdir}/bigboard/*
%dir %{python_sitelib}/bigboard
%{python_sitelib}/bigboard/*.so
%{python_sitelib}/bigboard/*.la
%{python_sitelib}/bigboard/*.py
%{python_sitelib}/bigboard/*.pyc
%{python_sitelib}/bigboard/*.pyo
%{python_sitelib}/bigboard/libbig/*.py
%{python_sitelib}/bigboard/libbig/*.pyc
%{python_sitelib}/bigboard/libbig/*.pyo
%{python_sitelib}/bigboard/httplib2/*.py
%{python_sitelib}/bigboard/httplib2/*.pyc
%{python_sitelib}/bigboard/httplib2/*.pyo
%{_datadir}/bigboard/stocks/*
%{_bindir}/bigboard

%changelog
* Tue May 15 2007 Colin Walters <walters@redhat.com> - 0.3.1-1
- Update
- Use python sitelib
- Remove unused gconf bits
- Use full source url 

* Mon May 14 2007 Colin Walters <walters@redhat.com> - 0.3-1
- Update 

* Mon Apr 30 2007 Colin Walters <walters@redhat.com> - 0.1.6-1
- Update 

* Mon Apr 30 2007 Colin Walters <walters@redhat.com> - 0.1.5-1
- Update 
- Bump some rpm requirements

* Thu Apr 19 2007 Colin Walters <walters@redhat.com> - 0.1.4-1
- Update 

* Wed Apr 18 2007 Colin Walters <walters@redhat.com> - 0.1.3-1
- Update 
- Require hippo-canvas-python

* Tue Apr 17 2007 Colin Walters <walters@redhat.com> - 0.1.2-1
- Update 

* Mon Apr 09 2007 Colin Walters <walters@redhat.com> - 0.1.1-1
- Update 

* Fri Mar 30 2007 Colin Walters <walters@redhat.com> - 0.1-1
- Initial public offering 

