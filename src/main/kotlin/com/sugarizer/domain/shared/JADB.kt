package com.sugarizer.domain.shared

import com.sugarizer.BuildConfig
import com.sugarizer.domain.model.DeviceEventModel
import com.sugarizer.domain.model.DeviceModel
import com.sugarizer.main.Main
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import net.dongliu.apk.parser.ApkFile
import se.vidstige.jadb.*
import se.vidstige.jadb.managers.PackageManager
import java.io.File
import java.net.ConnectException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class JADB {

    @Inject lateinit var bus: RxBus
    @Inject lateinit var stringUtils: StringUtils

    var listJadb = mutableListOf<JadbDevice>()
    var listDevice = mutableListOf<DeviceModel>()
    var connection: JadbConnection = JadbConnection()

    init {
        Main.appComponent.inject(this)

        startADB()
    }

    fun startADB() {
        var process: Process? = null

        when (OsCheck.operatingSystemType) {
            OsCheck.OSType.Windows -> { process = Runtime.getRuntime().exec(arrayOf("./adb/windows/platform-tools/adb.exe", "start-server")) }
            OsCheck.OSType.Linux -> { process = Runtime.getRuntime().exec(arrayOf("./adb/linux/platform-tools/adb", "start-server"))}
            OsCheck.OSType.MacOS -> { process = Runtime.getRuntime().exec(arrayOf("./adb/MACOS/platform-tools/adb", "start-server")) }
            OsCheck.OSType.Other -> { println("OS invalid") }
        }

        process?.let {
            println(convertStreamToString(it.inputStream))
        }
    }

    fun stopADB() {
        var process: Process? = null

        when (OsCheck.operatingSystemType) {
            OsCheck.OSType.Windows -> { process = Runtime.getRuntime().exec(arrayOf("./adb/windows/platform-tools/adb.exe", "kill-server")) }
            OsCheck.OSType.Linux -> { process = Runtime.getRuntime().exec(arrayOf("./adb/linux/platform-tools/adb", "kill-server"))}
            OsCheck.OSType.MacOS -> { process = Runtime.getRuntime().exec(arrayOf("./adb/MACOS/platform-tools/adb", "kill-server")) }
            OsCheck.OSType.Other -> { println("OS invalid") }
        }

        process?.let {
            println(convertStreamToString(it.inputStream))
        }
    }

    fun numberDevice(): Int {
        try {
            return connection.devices.size
        } catch (e: ConnectException) {
            return 0
        }
    }

    fun changeAction(device: JadbDevice, action: String) {
        var device = DeviceModel(device)

        device.setAction(action)

        bus.send(DeviceEventModel(DeviceEventModel.Status.CHANGED, device))
    }

    fun changeAction(name: String, action: String) {
//        for ((value, presentation.device) in list.withIndex()) {
//            if (presentation.device.name.get().equals(name)) {
//                //changeAction(list[value], action)
//            }
//        }
    }

    fun changeAction(index: Int, action: String) {
        if (connection.devices.size > index) {
            changeAction(connection.devices[index], action)
        }
    }

    fun watcher(): io.reactivex.Observable<DeviceEventModel> {
        return io.reactivex.Observable.create {
            subscriber -> run {

            try {
                var watcher: DeviceWatcher = connection.createDeviceWatcher(object : DeviceDetectionListener {
                    override fun onDetect(devices: MutableList<se.vidstige.jadb.JadbDevice>?) {
                        println("onDetect")

                        if (devices != null) {
                            listJadb.filter { !devices.contains(it) }
                                    .forEach {
                                        listJadb.remove(it)

                                        listDevice.filter {
                                            it.name.get().equals(it.name.get()) }
                                                .forEach {
                                                    listDevice.remove(it)
                                                }
                                        subscriber.onNext(DeviceEventModel(DeviceEventModel.Status.REMOVED, DeviceModel(it)))
                                    }

                            devices.filter { !listJadb.contains(it) }
                                    .forEach {
                                        var deviceModel: DeviceModel = DeviceModel(it)

                                        Observable.create<String> {
                                            while (isDeviceOffline(deviceModel.jadbDevice)) {
                                                println("Device Offline")
                                                Thread.sleep(1000)
                                            }

                                            it.onComplete()
                                        }
                                                .subscribeOn(Schedulers.computation())
                                                .observeOn(Schedulers.io())
                                                .doOnComplete {
                                                    println("On Complete")
                                                    hasPackage(deviceModel.jadbDevice, BuildConfig.APP_PACKAGE).subscribe {
                                                        if (it) {
                                                            startApp(deviceModel.jadbDevice).doOnComplete {
                                                                sendLog(deviceModel.jadbDevice, "Connected to Sugarizer Deployment Tool")
                                                            }.subscribe()
                                                        } else {
                                                            changeAction(deviceModel.jadbDevice, "Installing Application...")
                                                            installAPK(deviceModel.jadbDevice, File(BuildConfig.APK_LOCATION))
                                                                    .doOnComplete {
                                                                        startApp(deviceModel.jadbDevice)
                                                                                .doOnComplete {
                                                                                    sendLog(deviceModel.jadbDevice, "Connected to Sugarizer Deployment Tool")
                                                                                    changeAction(deviceModel.jadbDevice, "Application installed")
                                                                                }.subscribe()
                                                                    }.subscribe()
                                                        }
                                                    }
                                                }.subscribe()

                                        listJadb.add(it)
                                        listDevice.add(DeviceModel(it))

                                        subscriber.onNext(DeviceEventModel(DeviceEventModel.Status.ADDED, deviceModel))
                                    }
                        }
                    }

                    override fun onException(e: Exception?) {
                        if (e != null) {
                            println("onException: " + e.printStackTrace())
                        }
                    }
                })

                watcher.watch()
            } catch (e: java.net.ConnectException) {
                println("Error: Connection refused (adb is started ?)")
            }
        }
        }
    }

    fun convertStreamToString(`is`: java.io.InputStream): String {
        val s = java.util.Scanner(`is`).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }

    fun push(jadbDevice: JadbDevice, localFile: String, remoteFile: String) : Observable<String> {
        return Observable.create {
            jadbDevice.push(File(localFile), RemoteFile(remoteFile))

            it.onComplete()
        }
    }

    fun pull(jadbDevice: JadbDevice, remoteFile: String, localFile: String) : Observable<String> {
        return Observable.create {
            jadbDevice.pull(RemoteFile(remoteFile), File(localFile))

            it.onComplete()
        }
    }

    fun installAPK(jadbDevice: JadbDevice, file: File) : Observable<String> {
        return Observable.create {
            changeAction(jadbDevice, "Installing: " + file.name)
            sendLog(jadbDevice, "Installing: " + file.name)

            try {
                PackageManager(jadbDevice).install(file)
                changeAction(jadbDevice, file.name + " installed")
                sendLog(jadbDevice, file.name + " installed")
            } catch (e: JadbException) {
                changeAction(jadbDevice, file.name + " already installed")
                sendLog(jadbDevice, file.name + " already installed")
            } finally {
                it.onComplete()
            }
        }
    }

    fun remove(jadbDevice: JadbDevice, file: String) : Observable<String> {
        return Observable.create {
            PackageManager(jadbDevice).remove(RemoteFile(file))

            it.onComplete()
        }
    }

    fun getListPackage(jadbDevice: JadbDevice) : Observable<List<String>> {
        return Observable.create {
            var list: MutableList<String> = mutableListOf()
            var returnCMD: String = stringUtils.convertStreamToString(jadbDevice.executeShell(BuildConfig.CMD_LIST_PACKAGE))

            returnCMD.split("\n").forEach {
                list.add(it.removePrefix("package:"))
            }

            it.onNext(list)
        }
    }

    fun hasPackage(jadbDevice: JadbDevice, name: String) : Observable<Boolean> {
        return Observable.create { has ->
            run {
                getListPackage(jadbDevice).subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.io())
                        .doOnComplete { has.onComplete() }
                        .subscribe {
                            has.onNext(it.contains(name))

                            has.onComplete()
                        }
            }
        }
    }

    fun ping(jadbDevice: JadbDevice) {
        Observable.create<Any> {
            jadbDevice.executeShell(BuildConfig.CMD_PING)
            sendLog(jadbDevice, "Ping")
        }
                .subscribeOn(Schedulers.computation())
                .subscribe()
    }

    fun sendLog(jadbDevice: JadbDevice, send: String) {
        Observable.create<String> {
            it.onNext(stringUtils.convertStreamToString(jadbDevice.executeShell(BuildConfig.CMD_LOG + " --es extra_log \"" + send + "\"")))
        }
                .subscribeOn(Schedulers.computation())
                .subscribe {
                    println(it)
                }
    }

    fun startApp(jadbDevice: JadbDevice): Observable<Boolean> {
        return Observable.create {
            println(stringUtils.convertStreamToString(jadbDevice.executeShell("am start -n " + BuildConfig.APP_PACKAGE + "/" + BuildConfig.APP_PACKAGE + ".EmptyActivity")))

            it.onComplete()
        }
    }

    fun isDeviceOffline(jadbDevice: JadbDevice): Boolean {
        try {
            jadbDevice.state
            return false
        } catch (e: JadbException) {
            return true
        }
    }
}