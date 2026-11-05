package br.com.uri.campushub.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseServices {
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var usesEmulators = false

    @Synchronized
    fun useEmulatorsForE2ETests() {
        if (usesEmulators) return

        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
        firestore.useEmulator("10.0.2.2", 8080)
        usesEmulators = true
    }
}
