Name:           bigboard
Version:        0.1.2
Release:        1
Summary:        Sidebar application launcher using mugshot.org

Group:          Applications/Internet
License:        GPL
URL:            http://mugshot.org/
Source0:        bigboard-%{version}.tar.gz
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

Requires:       mugshot
Requires:       deskbar-applet
Requires:       hippo-canvas
Requires:       gnome-python2-desktop
Requires:       gnome-python2-gnomedesktop
Requires:       gnome-python2-gnomekeyring
Requires:       dbus-python


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
export GCONF_DISABLE_MAKEFILE_SCHEMA_INSTALL=1
make install DESTDIR=$RPM_BUILD_ROOT
unset GCONF_DISABLE_MAKEFILE_SCHEMA_INSTALL

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%doc COPYING
%attr(0755,root,root) %{_libexecdir}/bigboard/*
%{_libdir}/python*/site-packages/bigboard/*
%{_datadir}/bigboard/stocks/*
%{_bindir}/bigboard

%changelog
* Tue Apr 17 2007 Colin Walters <walters@redhat.com> - 0.1.2-1
- Update 

* Mon Apr 09 2007 Colin Walters <walters@redhat.com> - 0.1.1-1
- Update 

* Fri Mar 30 2007 Colin Walters <walters@redhat.com> - 0.1-1
- Initial public offering 

