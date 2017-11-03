/*
 COPYRIGHT 2016 ESRI
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.arcgis.apps.sharetolib

import android.content.Intent
import android.net.Uri
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.mapping.view.MapView
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class ExplorerUrlSchemeBuilder {

    constructor(mapView: MapView) {
        getFromMapView(mapView)
    }

    constructor(itemIdString: String) {
        if(itemIdString.isNullOrEmpty()) {
            throw RuntimeException("Map needs a portal item associated with it")
        }
        this.portalId = itemIdString
    }

    private val EXPLORER_SCHEME = "arcgis-explorer"

    private val CHARSET = "UTF-8"

    private val ITEMID_PARAM = "itemID"

    private val SCALE_PARAM = "scale"

    private val CENTER_PARAM = "center"

    private val ROTATION_PARAM = "rotation"

    private var useAddress = false

    private var address = ""

    private var rotation = 0.0

    private var portalId = ""

    private var scale = 200000.0

    private var centerX = 0.0

    private var centerY = 0.0

    private fun checkForValidPortalItem(mapView: MapView): String {
        if (!mapView.map.item.itemId.isNullOrEmpty()) {
            return mapView.map.item.itemId
        } else {
            throw RuntimeException("Map needs a portal item associated with it")
        }
    }

    fun getFromMapView(mapView: MapView): ExplorerUrlSchemeBuilder {

        this.portalId = checkForValidPortalItem(mapView)

        this.rotation = mapView.mapRotation

        this.scale = mapView.mapScale

        val center =  mapView.visibleArea.extent.center

        centerPoint(center)

        return this
    }

    fun itemId(itemId: String): ExplorerUrlSchemeBuilder {
        this.portalId = itemId
        return this
    }

    fun scale(scale: Double): ExplorerUrlSchemeBuilder {
        this.scale = scale
        return this
    }

    fun rotation(rotation: Double): ExplorerUrlSchemeBuilder {
        this.rotation = rotation
        return this
    }

    fun centerAddress(address: String): ExplorerUrlSchemeBuilder {
        this.useAddress = true
        this.address = address

        return this
    }

    fun centerPoint(x: Double, y: Double): ExplorerUrlSchemeBuilder {
        this.useAddress = false

        this.centerX = x
        this.centerY = y

        return this
    }

    fun centerPoint(centerPoint: Point): ExplorerUrlSchemeBuilder {
        this.useAddress = false

        if (centerPoint.spatialReference.equals(SpatialReferences.getWgs84())) {
            this.centerX = centerPoint.x
            this.centerY = centerPoint.y
        } else {
            val projectedGeom = GeometryEngine.project(centerPoint, SpatialReferences.getWgs84()) as Point
            this.centerX = projectedGeom.x
            this.centerY = projectedGeom.y
        }

        return this
    }

    private fun encode(value: String): String {
        try {
            return URLEncoder.encode(value, CHARSET)
        } catch (e: UnsupportedEncodingException) {
            return Uri.encode(value)
        }

    }

    fun buildUri(): Uri {
        val uri = Uri.Builder()
                .scheme(EXPLORER_SCHEME)
                .authority("")

        if (!portalId.isNullOrEmpty()) {
            uri.appendQueryParameter(ITEMID_PARAM, this.encode(portalId))
        } else {
            throw RuntimeException("Map needs a portal item associated with it")
        }

        if (useAddress) {
            uri.appendQueryParameter(CENTER_PARAM, this.encode(address))
        } else {
            val centerString = StringBuilder().append(centerY).append(",").append(centerX).toString()
            uri.appendQueryParameter(CENTER_PARAM, centerString)
        }

        uri.appendQueryParameter(ROTATION_PARAM, this.rotation.toString())

        uri.appendQueryParameter(SCALE_PARAM, this.scale.toString())

        return uri.build()
    }

    fun buildViewIntent(): Intent {
        val uri = buildUri()

        return Intent(Intent.ACTION_VIEW, uri)
    }

    fun buildShareIntent(): Intent {
        val uri = buildUri()

        var intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_SUBJECT, "Explorer Map")
        intent.putExtra(Intent.EXTRA_TEXT, uri.toString())
        intent.type = "text/plain"

        return intent
    }
}
