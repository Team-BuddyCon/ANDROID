package com.yapp.buddycon.data.network.response

import com.google.gson.annotations.SerializedName
import com.yapp.buddycon.domain.model.CouponInputInfo

data class GifticonInfoResponse(
    @SerializedName("id")
    val id: Long = -1,
    @SerializedName("imageUrl")
    val imageUrl: String = "",
    @SerializedName("barcode")
    val barcode: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("expireDate")
    val expireDate: String = "",
    @SerializedName("storeName")
    val storeName: String = "",
    @SerializedName("memo")
    val memo: String = "",
    @SerializedName("code")
    val couponStateMessage: String = ""
) {
    fun mapToCouponInputInfo() = CouponInputInfo(
        id = this.id,
        imageUrl = this.imageUrl,
        barcode = barcode,
        name = name,
        expireDate = expireDate,
        storeName = storeName,
        memo = memo
    )
}
