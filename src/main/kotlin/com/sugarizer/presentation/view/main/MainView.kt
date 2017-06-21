package view.main

import com.sugarizer.BuildConfig
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import com.sugarizer.domain.shared.JADB
import com.sugarizer.main.Main
import com.sugarizer.presentation.custom.ListMenuItem
import com.sugarizer.presentation.view.appmanager.AppManagerView
import com.sugarizer.presentation.view.createinstruction.CreateInstructionView
import com.sugarizer.presentation.view.device.DevicesView
import com.sugarizer.presentation.view.loadinstruction.LoadInstructionView
import com.sun.org.apache.bcel.internal.generic.LoadInstruction
import sun.security.krb5.Realm
import tornadofx.View
import tornadofx.action
import view.inventory.InventoryView
import java.io.IOException
import javax.inject.Inject
import kotlin.reflect.KClass

class MainView : View() {
    override val root : GridPane by fxml("/layout/main.fxml")

    @Inject lateinit var jadb: JADB

    val container : BorderPane by fxid(propName = "container")

    val inventory : ListMenuItem by fxid(propName = "inventory")
    val devices : ListMenuItem by fxid(propName = "devices")
    val application : ListMenuItem by fxid(propName = "appManager")
    val createInstruction : ListMenuItem by fxid(propName = "createInstruction")
    val loadInstruction: ListMenuItem by fxid(propName = "loadInstruction")

    private val views = mutableMapOf<Views, KClass<View>>()

    private enum class Views {
        DEVICES,
        INVENTORY,
        APP_MANAGER,
        CREATE_INSTRUCTION,
        LOAD_INSTRUCTION;

        override fun toString(): String {
            when (this) {
                DEVICES -> return "Devices"
                INVENTORY -> return "Inventory"
            }
            return super.toString()
        }
    }

    init {
        title = "Sugarizer Deloyment Tool - " + BuildConfig.VERSION

        Main.appComponent.inject(this)

        try {
            views.put(Views.DEVICES, DevicesView::class as KClass<View>)
            views.put(Views.INVENTORY, InventoryView::class as KClass<View>)
            views.put(Views.APP_MANAGER, AppManagerView::class as KClass<View>)
            views.put(Views.CREATE_INSTRUCTION, CreateInstructionView::class as KClass<View>)
            views.put(Views.LOAD_INSTRUCTION, LoadInstructionView::class as KClass<View>)

            devices.setOnMouseClicked { load(Views.DEVICES) }
            inventory.setOnMouseClicked { load(Views.INVENTORY) }
            application.setOnMouseClicked { load(Views.APP_MANAGER) }
            createInstruction.setOnMouseClicked { load(Views.CREATE_INSTRUCTION) }
            loadInstruction.setOnMouseClicked { load(Views.LOAD_INSTRUCTION) }

            load(Views.DEVICES)

            devices.setProgress(true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun load(view: Views) {
        if (views.containsKey(view)) {
            with(container) {
                center = find((views[view]) as KClass<View>).root
            }
        } else {
            println("Error: key not found in map")
        }
    }
}