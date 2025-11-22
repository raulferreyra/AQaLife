package com.urasweb.aqualife.ui.personal_info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PersonalInfoViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is personal Info Fragment"
    }
    val text: LiveData<String> = _text
}