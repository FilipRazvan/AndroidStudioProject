package com.example.chatappv2.model

class User {
    var uid:String? = null
    var name:String? = null
    var phoneNumber:String? = null
    var profileImage:String? = null

    constructor() {}
    constructor(
        uid:String?,
        name:String?,
        phoneNumber:String?,
        profileImage:String?
    ){
        this.uid = uid
        this.name = name
        this.profileImage = profileImage
        this.phoneNumber = phoneNumber
    }
}