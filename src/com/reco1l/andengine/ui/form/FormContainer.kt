package com.reco1l.andengine.ui.form

import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import org.anddev.andengine.entity.IEntity
import org.json.JSONArray
import org.json.JSONObject

open class FormContainer : UILinearContainer() {

    /**
     * Called when [submit] is called and the data has been encoded.
     */
    var onSubmit: ((data: JSONObject) -> Unit)? = null


    open fun onEncodeControlData(control: UIControl<*>): Any? {
        if (control is UISelect<*>) {
            return JSONArray(control.value)
        }
        return control.value
    }

    fun submit(): JSONObject {
        val json = JSONObject()

        fun IEntity.findFormControls() {
            forEach { child ->
                if (child is UIControl<*> && child.key != null) {
                    json.put(child.key!!, onEncodeControlData(child))
                } else {
                    child.findFormControls()
                }
            }
        }

        findFormControls()
        onSubmit?.invoke(json)
        return json
    }

}