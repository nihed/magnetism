from godmode import gnomepanel

NAME = 'OD'

PANEL_ID = 'bottom_panel_' + NAME

def setup_panels():
    gnomepanel.create_toplevel(PANEL_ID)
    gnomepanel.add_applet(PANEL_ID, 'bigboard_' + NAME, 'BigBoard_Applet', 1)
    gnomepanel.add_applet(PANEL_ID, 'windowlist_' + NAME, 'GNOME_WindowListApplet', 2)
    gnomepanel.add_applet(PANEL_ID, 'clock_' + NAME, 'GNOME_ClockApplet', 1, right=True)
    gnomepanel.add_applet(PANEL_ID, 'tray_' + NAME, 'GNOME_SystemTrayApplet', 0, right=True)
    gnomepanel.set_toplevels([PANEL_ID])