%{!?python_sitelib: %define python_sitelib %(%{__python} -c "from distutils.sysconfig import get_python_lib; print get_python_lib()")}
%{!?python_sitearch: %define python_sitearch %(%{__python} -c "from distutils.sysconfig import get_python_lib; print get_python_lib(1)")}

Name:           online-desktop
Version:        0.2.5
Release:        1%{?dist}
Summary:        Desktop built around web sites and online services

Group:          Applications/Internet
License:        GPL
URL:            http://developer.mugshot.org/
Source0:        http://download.mugshot.org/online-desktop/sources/online-desktop-%{version}.tar.gz
BuildRoot:      %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
BuildArch:	noarch
Requires:	dbus-python
Requires:	gnome-python2-gconf
Requires:	bigboard >= 0.4.0

BuildRequires:	intltool
BuildRequires:	python-devel
BuildRequires:  glib2-devel
BuildRequires:  desktop-file-utils

%description

The "online desktop" is a flavor of the GNOME desktop built around web
sites and online services. This package contains a grab-bag of
integration points with various sites and services, such as mailto:
handlers, .desktop files, and so forth.

%package flickr
Summary: Flickr integration for your desktop
Group: Applications/Internet
%description flickr
Contains a menu entry for the Flickr photo sharing site.

%package gmail
Summary: GMail integration for your desktop
Group: Applications/Internet
# requires mailto-handler (which is theoretically also required by other web mail)
Requires: online-desktop
%description gmail
Contains a menu entry for GMail.

%package google-calendar
Summary: Google Calendar integration for your desktop
Group: Applications/Internet
Requires: online-desktop
%description google-calendar
Contains a menu entry for Google Calendar.

%package google-docs
Summary: Google Docs and Spreadsheets integration for your desktop
Group: Applications/Internet
Requires: online-desktop
%description google-docs
Contains a menu entry for Google Docs and Spreadsheets.

%package google-reader
Summary: Google Reader integration for your desktop
Group: Applications/Internet
Requires: online-desktop
%description google-reader
Contains a menu entry for Google Reader.

%prep
%setup -q

%build
%configure
make %{?_smp_mflags}

%install
rm -rf $RPM_BUILD_ROOT
make install DESTDIR=$RPM_BUILD_ROOT

desktop-file-install --vendor="fedora"                  \
  --dir=${RPM_BUILD_ROOT}%{_datadir}/applications       \
  --delete-original                                     \
  ${RPM_BUILD_ROOT}/%{_datadir}/applications/*.desktop

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%{_bindir}/god-mode
# not used yet
%{_datadir}/applications/fedora-online-desktop.desktop
%exclude %{_datadir}/icons/hicolor/*/apps/picasa.png
%exclude %{_datadir}/icons/hicolor/*/apps/yahoo-mail.png
%dir %{python_sitelib}/godmode
%{python_sitelib}/godmode/*
%doc COPYING README

%files flickr
%defattr(-,root,root,-)
%{_datadir}/applications/fedora-flickr.desktop
%{_datadir}/icons/hicolor/*/apps/flickr.png
%doc README

%files gmail
%defattr(-,root,root,-)
%{_datadir}/applications/fedora-gmail.desktop
%{_datadir}/icons/hicolor/*/apps/gmail.png
%doc README

%files google-calendar
%defattr(-,root,root,-)
%{_datadir}/applications/fedora-google-calendar.desktop
%{_datadir}/icons/hicolor/*/apps/google-calendar.png
%doc README

%files google-docs
%defattr(-,root,root,-)
%{_datadir}/applications/fedora-google-docs.desktop
%{_datadir}/icons/hicolor/*/apps/google-docs.png
%doc README

%files google-reader
%defattr(-,root,root,-)
%{_datadir}/applications/fedora-google-reader.desktop
%{_datadir}/icons/hicolor/*/apps/google-reader.png
%doc README

%post
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%postun
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%post flickr
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%postun flickr
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%post gmail
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%postun gmail
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%post google-calendar
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%postun google-calendar
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%post google-docs
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%postun google-docs
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%post google-reader
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%postun google-reader
touch --no-create %{_datadir}/icons/hicolor || :
%{_bindir}/gtk-update-icon-cache --quiet %{_datadir}/icons/hicolor || :

%changelog
* Tue Jun 05 2007 Colin Walters <walters@redhat.com> - 0.2.5-1
- update
- add gtk-update-icon-cache for all pkgs

* Fri Jun 01 2007 Colin Walters <walters@redhat.com> - 0.2.4-1
- Update
- Icon cache update in post/postun
- Install desktop files in post
- Exclude unused desktop files
- Require newest bigboard
- Typo in sources
- Add subpackage requires

* Thu May 31 2007 Colin Walters <walters@redhat.com> - 0.2.1-1
- Update
- Require bigboard
- Fix dbus-python dep

* Fri May 25 2007 Colin Walters <walters@redhat.com> - 0.2-1
- Add god-mode

* Tue May  1 2007 Havoc Pennington <hp@redhat.com> - 0.1-2
- add subpackage for each user-visible app

* Thu Apr 26 2007 Havoc Pennington <hp@redhat.com> - 0.1-1
- created package

