
/* Generated data (by glib-mkenums) */

#include "hippo-canvas-type-builtins.h"


/* enumerations from "hippo/hippo-canvas-box.h" */
#include "hippo/hippo-canvas-box.h"
const GEnumValue _hippo_pack_flags_values[] = {
  { HIPPO_PACK_EXPAND, "HIPPO_PACK_EXPAND", "expand" },
  { HIPPO_PACK_END, "HIPPO_PACK_END", "end" },
  { HIPPO_PACK_FIXED, "HIPPO_PACK_FIXED", "fixed" },
  { HIPPO_PACK_IF_FITS, "HIPPO_PACK_IF_FITS", "if-fits" },
  { HIPPO_PACK_FLOAT_LEFT, "HIPPO_PACK_FLOAT_LEFT", "float-left" },
  { HIPPO_PACK_FLOAT_RIGHT, "HIPPO_PACK_FLOAT_RIGHT", "float-right" },
  { HIPPO_PACK_CLEAR_LEFT, "HIPPO_PACK_CLEAR_LEFT", "clear-left" },
  { HIPPO_PACK_CLEAR_RIGHT, "HIPPO_PACK_CLEAR_RIGHT", "clear-right" },
  { HIPPO_PACK_CLEAR_BOTH, "HIPPO_PACK_CLEAR_BOTH", "clear-both" },
  { 0, NULL, NULL }
};

GType
hippo_pack_flags_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoPackFlags", _hippo_pack_flags_values);

  return type;
}

const GEnumValue _hippo_cascade_mode_values[] = {
  { HIPPO_CASCADE_MODE_NONE, "HIPPO_CASCADE_MODE_NONE", "none" },
  { HIPPO_CASCADE_MODE_INHERIT, "HIPPO_CASCADE_MODE_INHERIT", "inherit" },
  { 0, NULL, NULL }
};

GType
hippo_cascade_mode_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoCascadeMode", _hippo_cascade_mode_values);

  return type;
}


/* enumerations from "hippo/hippo-canvas-context.h" */
#include "hippo/hippo-canvas-context.h"
const GEnumValue _hippo_stock_color_values[] = {
  { HIPPO_STOCK_COLOR_BG_NORMAL, "HIPPO_STOCK_COLOR_BG_NORMAL", "normal" },
  { HIPPO_STOCK_COLOR_BG_PRELIGHT, "HIPPO_STOCK_COLOR_BG_PRELIGHT", "prelight" },
  { 0, NULL, NULL }
};

GType
hippo_stock_color_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoStockColor", _hippo_stock_color_values);

  return type;
}


/* enumerations from "hippo/hippo-canvas-item.h" */
#include "hippo/hippo-canvas-item.h"
const GEnumValue _hippo_canvas_pointer_values[] = {
  { HIPPO_CANVAS_POINTER_UNSET, "HIPPO_CANVAS_POINTER_UNSET", "unset" },
  { HIPPO_CANVAS_POINTER_DEFAULT, "HIPPO_CANVAS_POINTER_DEFAULT", "default" },
  { HIPPO_CANVAS_POINTER_HAND, "HIPPO_CANVAS_POINTER_HAND", "hand" },
  { 0, NULL, NULL }
};

GType
hippo_canvas_pointer_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoCanvasPointer", _hippo_canvas_pointer_values);

  return type;
}

const GEnumValue _hippo_item_alignment_values[] = {
  { HIPPO_ALIGNMENT_FILL, "HIPPO_ALIGNMENT_FILL", "fill" },
  { HIPPO_ALIGNMENT_START, "HIPPO_ALIGNMENT_START", "start" },
  { HIPPO_ALIGNMENT_CENTER, "HIPPO_ALIGNMENT_CENTER", "center" },
  { HIPPO_ALIGNMENT_END, "HIPPO_ALIGNMENT_END", "end" },
  { 0, NULL, NULL }
};

GType
hippo_item_alignment_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoItemAlignment", _hippo_item_alignment_values);

  return type;
}


/* enumerations from "hippo/hippo-canvas-text.h" */
#include "hippo/hippo-canvas-text.h"
const GEnumValue _hippo_canvas_size_mode_values[] = {
  { HIPPO_CANVAS_SIZE_FULL_WIDTH, "HIPPO_CANVAS_SIZE_FULL_WIDTH", "full-width" },
  { HIPPO_CANVAS_SIZE_WRAP_WORD, "HIPPO_CANVAS_SIZE_WRAP_WORD", "wrap-word" },
  { HIPPO_CANVAS_SIZE_ELLIPSIZE_END, "HIPPO_CANVAS_SIZE_ELLIPSIZE_END", "ellipsize-end" },
  { 0, NULL, NULL }
};

GType
hippo_canvas_size_mode_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoCanvasSizeMode", _hippo_canvas_size_mode_values);

  return type;
}


/* enumerations from "hippo/hippo-canvas-widgets.h" */
#include "hippo/hippo-canvas-widgets.h"
const GEnumValue _hippo_scrollbar_policy_values[] = {
  { HIPPO_SCROLLBAR_NEVER, "HIPPO_SCROLLBAR_NEVER", "never" },
  { HIPPO_SCROLLBAR_AUTOMATIC, "HIPPO_SCROLLBAR_AUTOMATIC", "automatic" },
  { HIPPO_SCROLLBAR_ALWAYS, "HIPPO_SCROLLBAR_ALWAYS", "always" },
  { 0, NULL, NULL }
};

GType
hippo_scrollbar_policy_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoScrollbarPolicy", _hippo_scrollbar_policy_values);

  return type;
}


/* enumerations from "hippo/hippo-event.h" */
#include "hippo/hippo-event.h"
const GEnumValue _hippo_event_type_values[] = {
  { HIPPO_EVENT_BUTTON_PRESS, "HIPPO_EVENT_BUTTON_PRESS", "button-press" },
  { HIPPO_EVENT_BUTTON_RELEASE, "HIPPO_EVENT_BUTTON_RELEASE", "button-release" },
  { HIPPO_EVENT_MOTION_NOTIFY, "HIPPO_EVENT_MOTION_NOTIFY", "motion-notify" },
  { HIPPO_EVENT_KEY_PRESS, "HIPPO_EVENT_KEY_PRESS", "key-press" },
  { 0, NULL, NULL }
};

GType
hippo_event_type_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoEventType", _hippo_event_type_values);

  return type;
}

const GEnumValue _hippo_motion_detail_values[] = {
  { HIPPO_MOTION_DETAIL_ENTER, "HIPPO_MOTION_DETAIL_ENTER", "enter" },
  { HIPPO_MOTION_DETAIL_LEAVE, "HIPPO_MOTION_DETAIL_LEAVE", "leave" },
  { HIPPO_MOTION_DETAIL_WITHIN, "HIPPO_MOTION_DETAIL_WITHIN", "within" },
  { 0, NULL, NULL }
};

GType
hippo_motion_detail_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoMotionDetail", _hippo_motion_detail_values);

  return type;
}

const GEnumValue _hippo_key_values[] = {
  { HIPPO_KEY_UNKNOWN, "HIPPO_KEY_UNKNOWN", "unknown" },
  { HIPPO_KEY_RETURN, "HIPPO_KEY_RETURN", "return" },
  { HIPPO_KEY_ESCAPE, "HIPPO_KEY_ESCAPE", "escape" },
  { 0, NULL, NULL }
};

GType
hippo_key_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoKey", _hippo_key_values);

  return type;
}


/* enumerations from "hippo/hippo-graphics.h" */
#include "hippo/hippo-graphics.h"
const GEnumValue _hippo_orientation_values[] = {
  { HIPPO_ORIENTATION_VERTICAL, "HIPPO_ORIENTATION_VERTICAL", "vertical" },
  { HIPPO_ORIENTATION_HORIZONTAL, "HIPPO_ORIENTATION_HORIZONTAL", "horizontal" },
  { 0, NULL, NULL }
};

GType
hippo_orientation_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoOrientation", _hippo_orientation_values);

  return type;
}

const GEnumValue _hippo_side_values[] = {
  { HIPPO_SIDE_TOP, "HIPPO_SIDE_TOP", "top" },
  { HIPPO_SIDE_BOTTOM, "HIPPO_SIDE_BOTTOM", "bottom" },
  { HIPPO_SIDE_LEFT, "HIPPO_SIDE_LEFT", "left" },
  { HIPPO_SIDE_RIGHT, "HIPPO_SIDE_RIGHT", "right" },
  { 0, NULL, NULL }
};

GType
hippo_side_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoSide", _hippo_side_values);

  return type;
}

const GEnumValue _hippo_gravity_values[] = {
  { HIPPO_GRAVITY_NORTH_WEST, "HIPPO_GRAVITY_NORTH_WEST", "north-west" },
  { HIPPO_GRAVITY_NORTH_EAST, "HIPPO_GRAVITY_NORTH_EAST", "north-east" },
  { HIPPO_GRAVITY_SOUTH_EAST, "HIPPO_GRAVITY_SOUTH_EAST", "south-east" },
  { HIPPO_GRAVITY_SOUTH_WEST, "HIPPO_GRAVITY_SOUTH_WEST", "south-west" },
  { 0, NULL, NULL }
};

GType
hippo_gravity_get_type (void)
{
  static GType type = 0;

  if (G_UNLIKELY (type == 0))
    type = g_enum_register_static ("HippoGravity", _hippo_gravity_values);

  return type;
}


/* Generated data ends here */

