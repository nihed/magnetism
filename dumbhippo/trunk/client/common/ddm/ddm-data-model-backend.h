/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */

#ifndef __DDM_DATA_MODEL_BACKEND_H__
#define __DDM_DATA_MODEL_BACKEND_H__

#include <ddm/ddm-data-model.h>

G_BEGIN_DECLS

struct _DDMDataModelBackend
{
    void     (* add_model)      (DDMDataModel *model,
                                 void         *backend_data);
    void     (* remove_model)   (DDMDataModel *model,
                                 void         *backend_data);
    
    void     (* send_query)     (DDMDataModel *model,
                                 DDMDataQuery *query,
                                 void         *backend_data);
    
    void     (* send_update)    (DDMDataModel *model,
                                 DDMDataQuery *query,
                                 void         *backend_data);

    /* Do idle processing; at the beginning of ddm_data_model_flush */
    void     (* flush)          (DDMDataModel *model,
                                 void         *backend_data);

    GCallback _ddm_padding_1;
    GCallback _ddm_padding_2;
    GCallback _ddm_padding_3;
    GCallback _ddm_padding_4;
    GCallback _ddm_padding_5;
    GCallback _ddm_padding_6;
    GCallback _ddm_padding_7;
};

G_END_DECLS

#endif /* __DDM_DATA_MODEL_BACKEND_H__ */
