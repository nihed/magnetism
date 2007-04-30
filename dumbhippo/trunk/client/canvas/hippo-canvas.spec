%{!?python_sitearch: %define python_sitearch %(%{__python} -c "from distutils.sysconfig import get_python_lib; print get_python_lib(1)")}

Name:           hippo-canvas
Version:        0.2.16
Release:        1%{?dist}
Summary:        A canvas widget

Group:          System Environment/Libraries
License:        GPL
URL:            http://developer.mugshot.org/wiki/Hippo_Canvas
Source0:        http://download.mugshot.org/extras/canvas/source/hippo-canvas-%{version}.tar.gz
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

BuildRequires:  cairo-devel
BuildRequires:  pango-devel
BuildRequires:  glib2-devel
BuildRequires:  gtk2-devel

%description
The hippo-canvas library contains a canvas widget developed by the 
Mugshot team for displaying GTK+ UI across multiple platforms

%package        devel
Summary:        Development files for hippo-canvas
Group:          Development/Libraries
Requires:       %{name} = %{version}-%{release}

%description    devel
The hippo-canvas-devel package contains libraries and header files for
developing applications that use hippo-canvas

%package        python
Summary:        Python module for hippo-canvas
Group:          Development/Languages
Requires:       %{name} = %{version}-%{release}
BuildRequires:  python-devel

%description    python 
The hippo-canvas-python package contains a Python interface.

%prep
%setup -q


%build
%configure --disable-static
make %{?_smp_mflags}


%install
rm -rf $RPM_BUILD_ROOT
make install DESTDIR=$RPM_BUILD_ROOT
find $RPM_BUILD_ROOT -name '*.la' -exec rm -f {} ';'


%clean
rm -rf $RPM_BUILD_ROOT


%post -p /sbin/ldconfig

%postun -p /sbin/ldconfig


%files
%defattr(-,root,root,-)
%doc LICENSE README AUTHORS
%{_libdir}/*.so.*

%files devel
%defattr(-,root,root,-)
%{_includedir}/*
%{_libdir}/*.so

%files python
%defattr(-,root,root,-)
%{python_sitearch}/*.so

%changelog
* Mon Apr 30 2007 Colin Walters <walters@redhat.com> - 0.2.16-1
- New upstream

* Mon Apr 09 2007 Colin Walters <walters@redhat.com> - 0.2.13-2
- Fully qualify source url

* Tue Apr 03 2007 Colin Walters <walters@redhat.com> - 0.2.13-1
- Tweak for mugshot.org releases

* Tue Oct 03 2006 John (J5) Palmieri <johnp@redhat.com> - 0.1.2-1
- initial package
