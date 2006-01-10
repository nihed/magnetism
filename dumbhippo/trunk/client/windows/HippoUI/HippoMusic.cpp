#include "StdAfx.h"
#include "HippoMusic.h"

HippoMusic::HippoMusic()
{
	iTunes_ = HippoMusicMonitor::createITunesMonitor();
}

HippoMusic::~HippoMusic()
{
	delete iTunes_;
}
