/*
 * These abstract interfaces are intended to hide iTunes/WMP/etc. (various ways someone might be playing music)
 */
#pragma once

#include <assert.h>
#include "HippoArray.h"
#include "HippoUtil.h"

class HippoTrackInfo
{
public:
	HippoTrackInfo() 
		: name_(L""), artist_(L"") {

	}

	bool hasName() const { return name_.Length() > 0; }
	const HippoBSTR& getName() const { assert(hasName()); return name_; }
	void setName(const HippoBSTR& val) { name_ = val; }
	bool hasArtist() const { return artist_.Length() > 0; }
	const HippoBSTR& getArtist() const { assert(hasArtist()); return artist_; }
	void setArtist(const HippoBSTR& val) { artist_ = val; }

private:
	HippoBSTR name_;
	HippoBSTR artist_;
};

class HippoMusicListener;

class HippoMusicMonitor
{
public:
	
	virtual bool hasCurrentTrack() const = 0;
	virtual const HippoTrackInfo& getCurrentTrack() const = 0;
	void addListener(HippoMusicListener *listener);
	void removeListener(HippoMusicListener *listener);

	virtual ~HippoMusicMonitor() {}

	static HippoMusicMonitor* createITunesMonitor();

protected:
	void fireCurrentTrackChanged(bool haveTrack, const HippoTrackInfo & newTrack);

	HippoMusicMonitor() {}

private:
	HippoArray<HippoMusicListener*> listeners_;

	// private so they aren't used
	HippoMusicMonitor(const HippoMusicMonitor &other);
	HippoMusicMonitor& operator=(const HippoMusicMonitor &other);
};

class HippoMusicListener
{
public:
	virtual void onCurrentTrackChanged(HippoMusicMonitor *monitor, bool haveTrack, const HippoTrackInfo & newTrack) = 0;
	virtual ~HippoMusicListener() {}
};
