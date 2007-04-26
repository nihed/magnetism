Name:           online-desktop
Version:        0.1
Release:        1%{?dist}
Summary:        Desktop built around web sites and online services

Group:          Applications/Internet
License:        GPL
URL:            http://developer.mugshot.org/
Source0:        online-desktop-%{version}.tar.gz
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

BuildRequires:  glib2-devel
#Requires:       

%description

Desktop built around web sites and online services. This package
contains a grab-bag of integration points with various sites and
services, such as mailto: handlers, .desktop files, and so forth.

%prep
%setup -q


%build
%configure
make %{?_smp_mflags}


%install
rm -rf $RPM_BUILD_ROOT
make install DESTDIR=$RPM_BUILD_ROOT


%clean
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root,-)
%{_bindir}/*
%{_datadir}/applications/*.desktop

%doc



%changelog
* Thu Apr 26 2007 Havoc Pennington <hp@redhat.com> - 0.1-1
- created package

