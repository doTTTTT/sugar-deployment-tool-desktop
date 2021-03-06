package com.sugarizer.view.device.cellfactory

import com.sugarizer.listitem.ListItemInstruction
import org.controlsfx.control.GridCell

class ListItemInstructionCell : GridCell<ListItemInstruction>() {
    override fun updateItem(item: ListItemInstruction?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
        } else {
            graphic = item
        }
    }
}