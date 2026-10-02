package br.com.uri.campushub

import com.google.firebase.firestore.FirebaseFirestore

object FirebaseServices {
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
}
