package com.example.imu_gravity_sensor_stream

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class SharedViewModel : ViewModel() {
    val gravityData = MutableLiveData<FloatArray>()
    val isWifiConnected = MutableLiveData<Boolean>()

    val isStreaming = MutableLiveData<Boolean>()

    val portLiveData: MutableLiveData<Int> = MutableLiveData(5555)

    companion object {
        // Singleton for exposing LiveData to the Service
        private val _gravityData = MutableLiveData<FloatArray>()
        val gravityData: LiveData<FloatArray> = _gravityData

        fun setGravityData(data: FloatArray) {
            _gravityData.postValue(data)
        }
    }
}