package io.github.takusan23.android12getipaddress

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.activity_main_text_view)
        lifecycleScope.launch {
            collectIpAddress(this@MainActivity).collect {
                textView.text = it
            }
        }
    }

    /**
     * IPアドレスを取得する関数
     *
     * @param context Context
     * @return IPv4のIPアドレス
     * */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getIpAddress(context: Context): String? {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
        println(linkProperties?.linkAddresses)
        val localIpAddress = linkProperties?.linkAddresses
            ?.find { it.address?.toString()?.contains("192") == true }
            ?.address
            ?.hostAddress
        return localIpAddress
    }

    /**
     * IPアドレスをFlowで受け取る
     *
     * @param context Context
     * @return IPv4のIPアドレス
     * */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun collectIpAddress(context: Context) = callbackFlow {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        // コールバック
        val networkCallback: ConnectivityManager.NetworkCallback
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val linkProperties = connectivityManager.getLinkProperties(network)
                // IPv4アドレスを探す
                val address = linkProperties?.linkAddresses
                    ?.find { it.address?.toString()?.contains("192") == true }
                    ?.address?.hostAddress
                if (address != null) {
                    trySend(address)
                }
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

}