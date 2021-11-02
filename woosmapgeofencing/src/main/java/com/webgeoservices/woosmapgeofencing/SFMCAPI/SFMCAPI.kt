package com.webgeoservices.woosmapgeofencing.SFMCAPI

import android.content.Context
import android.util.Log
import com.android.volley.*

import org.json.JSONException
import org.json.JSONObject

import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.webgeoservices.woosmapgeofencing.WoosmapSettings


class SFMCAPI(val context: Context) {
    private var requestQueue: RequestQueue? = null
    private var eventDefinitionKey: String = ""

    fun pushDatatoMC(data: HashMap<String, Any>?, event: String) {
        eventDefinitionKey = event
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }

        if(WoosmapSettings.SFMCAccessToken == "") {
            refreshAccessToken(data)
        } else {
            val params = JSONObject()
            try {
                params.put("ContactKey", WoosmapSettings.SFMCCredentials.get("contactKey"))
                params.put("EventDefinitionKey", eventDefinitionKey)

                //convert to string using gson
                val gson = Gson()
                val dataHashMapString = gson.toJson(data)
                val dataJson = JSONObject(dataHashMapString)

                params.put("Data", dataJson)
            } catch (ignored: JSONException) {
                // never thrown in this case
            }

            val postRequest: JsonObjectRequest = object :
                JsonObjectRequest(
                    Method.POST,
                    WoosmapSettings.SFMCCredentials.get("restBaseURI")+"/interaction/v1/events",
                    params,
                    { response ->
                        Log.d(WoosmapSettings.Tags.WoosmapSdkTag,"Data send to SFMC" + response)
                    },
                    Response.ErrorListener { error ->
                        if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                            refreshAccessToken(data)
                        } else {
                            val response = error.networkResponse
                            val jsonError = String(response.data)
                            Log.d(WoosmapSettings.Tags.WoosmapSdkTag,"Error SFMC:" + jsonError)
                        }
                    }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers: MutableMap<String, String> = HashMap()
                    headers["Authorization"] = "Bearer ${WoosmapSettings.SFMCAccessToken}"
                    return headers
                }
            }
            requestQueue?.add(postRequest)
        }
    }

    fun refreshAccessToken(data: HashMap<String, Any>?) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context)
        }
        val params = JSONObject()
        try {
            params.put("client_id", WoosmapSettings.SFMCCredentials.get("client_id"))
            params.put("client_secret", WoosmapSettings.SFMCCredentials.get("client_secret"))
            params.put("grant_type", "client_credentials")
            params.put("scope", "list_and_subscribers_read journeys_read")
            params.put("account_id", WoosmapSettings.SFMCCredentials.get("account_id"))
        } catch (ignored: JSONException) {
            // never thrown in this case
        }
        val refreshTokenRequest = JsonObjectRequest(
            Request.Method.POST,
            WoosmapSettings.SFMCCredentials.get("authenticationBaseURI") + "/v2/Token",
            params,
            { response ->
                try {
                    WoosmapSettings.SFMCAccessToken = response.getString("access_token")
                    pushDatatoMC(data,eventDefinitionKey)
                    WoosmapSettings.saveSettings(context)
                } catch (e: JSONException) {
                    Log.d(WoosmapSettings.Tags.WoosmapSdkTag,"Error SFMC:" + e)
                }
            })
            { error -> // show error to user. refresh failed.
                 try {
                    if (error.networkResponse != null && !error.networkResponse.data.isEmpty()) {
                        Log.d(
                            WoosmapSettings.Tags.WoosmapSdkTag,
                            "Error on token refresh SFMC:" + String(error.networkResponse.data)
                        )
                    } else {
                        Log.d(
                            WoosmapSettings.Tags.WoosmapSdkTag,
                            "Error on token refresh SFMC " + error.message)
                    }
                } catch (e: JSONException) {
                    Log.d(WoosmapSettings.Tags.WoosmapSdkTag,"Error SFMC:" + e)
                }
            }
        requestQueue?.add(refreshTokenRequest)
    }
}