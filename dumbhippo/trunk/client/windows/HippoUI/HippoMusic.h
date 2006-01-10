#pragma once
#include "HippoMusicMonitor.h"

/*
 * Creates and manages all the music monitor thingies, for now only iTunes is supported 
 */
class HippoMusic
{
public:
	HippoMusic();
	~HippoMusic();

private:
	HippoMusicMonitor *iTunes_;
};
