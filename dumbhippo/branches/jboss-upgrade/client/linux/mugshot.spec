Name:           mugshot
Version:        1.1.11
Release:        1%{?dist}
Summary:        Companion software for mugshot.org

Group:          Applications/Internet
License:        GPL
URL:            http://mugshot.org/
## FIXME get the full download url
Source0:        mugshot-%{version}.tar.gz
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

BuildRequires:  glib2-devel >= 2.6
BuildRequires:  gtk2-devel >= 2.6
BuildRequires:  loudmouth-devel >= 1.0.3-3
BuildRequires:  dbus-devel >= 0.61
BuildRequires:  curl-devel >= 7.15
BuildRequires:  GConf2-devel >= 2.8

# 1.0.3-3 has a backport from 1.0.4 to fix various segfaults
Requires:       loudmouth >= 1.0.3-3

%description
Mugshot works with the server at mugshot.org to extend 
the panel, web browser, music player and other parts of the desktop with 
a "live social experience." It's fun and easy.


%prep
%setup -q


%build
%configure
make %{?_smp_mflags}


%install
rm -rf $RPM_BUILD_ROOT
export GCONF_DISABLE_MAKEFILE_SCHEMA_INSTALL=1
make install DESTDIR=$RPM_BUILD_ROOT
unset GCONF_DISABLE_MAKEFILE_SCHEMA_INSTALL

%clean
rm -rf $RPM_BUILD_ROOT

%post
export GCONF_CONFIG_SOURCE=`gconftool-2 --get-default-source`
SCHEMAS="mugshot-uri-handler.schemas"
for S in $SCHEMAS; do
  gconftool-2 --makefile-install-rule %{_sysconfdir}/gconf/schemas/$S > /dev/null
done

touch --no-create %{_datadir}/icons/hicolor
if [ -x /usr/bin/gtk-update-icon-cache ]; then
  gtk-update-icon-cache -q %{_datadir}/icons/hicolor
fi

%postun
touch --no-create %{_datadir}/icons/hicolor
if [ -x /usr/bin/gtk-update-icon-cache ]; then
  gtk-update-icon-cache -q %{_datadir}/icons/hicolor
fi


%files
%defattr(-,root,root,-)
%doc

%{_bindir}/mugshot
%{_bindir}/mugshot-uri-handler
%{_datadir}/icons/hicolor/16x16/apps/*.png
%{_datadir}/icons/hicolor/16x16/apps/*.gif
%{_datadir}/icons/hicolor/22x22/apps/*.png
%{_datadir}/icons/hicolor/24x24/apps/*.gif
%{_datadir}/icons/hicolor/32x32/apps/*.gif
%{_datadir}/icons/hicolor/48x48/apps/*.gif
%{_datadir}/icons/hicolor/128x128/apps/*.png
%{_datadir}/mugshot
%{_datadir}/gnome/autostart/mugshot.desktop
%{_sysconfdir}/gconf/schemas/*.schemas

%changelog
* Wed Jul 19 2006 Colin Walters <walters@redhat.com> - 1.1.11-1
- 1.1.11

* Wed Jul 19 2006 Colin Walters <walters@redhat.com> - 1.1.10-1
- 1.1.10

* Sat Jul 15 2006 Havoc Pennington <hp@redhat.com> - 1.1.9-1
- 1.1.9

* Thu Jul 13 2006 Havoc Pennington <hp@redhat.com> - 1.1.8-1
- 1.1.8

* Tue Jul 11 2006 Havoc Pennington <hp@redhat.com> - 1.1.7-1
- 1.1.7

* Wed Jun 28 2006 Havoc Pennington <hp@redhat.com> - 1.1.6-1
- 1.1.6

* Fri Jun  9 2006 Havoc Pennington <hp@redhat.com> - 1.1.5-1
- 1.1.5

* Mon May 29 2006 Havoc Pennington <hp@redhat.com> - 1.1.3-1
- 1.1.3

* Sat May 27 2006 Havoc Pennington <hp@redhat.com> - 1.1.2-2
- add requirement on patched loudmouth

* Fri May 26 2006 Havoc Pennington <hp@redhat.com> - 1.1.2-1
- 1.1.2

* Mon May 22 2006 Havoc Pennington <hp@redhat.com> - 1.1.1-1
- 1.1.1

* Mon May 22 2006 Havoc Pennington <hp@redhat.com> - 1.1.0-1
- Initial package

