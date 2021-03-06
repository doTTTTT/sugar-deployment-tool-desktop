package com.sugarizer.view.device.cellfactory

import com.sugarizer.listitem.ListItemSpk
import javafx.geometry.Insets
import org.controlsfx.control.GridCell

class ListItemSpkCell : GridCell<ListItemSpk>() {
    override fun updateItem(item: ListItemSpk?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
        } else {
            width += 50
            padding = Insets(0.0, 50.0, 10.0, 10.0)
            graphic = item
        }
    }
}