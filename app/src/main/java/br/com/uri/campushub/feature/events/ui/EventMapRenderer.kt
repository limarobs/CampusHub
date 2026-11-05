package br.com.uri.campushub.feature.events.ui

import android.view.View
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.events.model.CampusEvent
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

internal object EventMapRenderer {
    fun render(root: View, event: CampusEvent) {
        val container = root.findViewById<View>(R.id.eventMapContainer)
        val latitude = event.latitude
        val longitude = event.longitude
        if (latitude == null || longitude == null || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            container.visibility = View.GONE
            return
        }

        val point = Point.fromLngLat(longitude, latitude)
        root.findViewById<MapView>(R.id.eventMapView).also { mapView ->
            mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                mapView.annotations.createPointAnnotationManager().create(
                    PointAnnotationOptions().withPoint(point).withTextField(event.title)
                )
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder().center(point).zoom(16.0).build()
                )
            }
        }
    }
}
