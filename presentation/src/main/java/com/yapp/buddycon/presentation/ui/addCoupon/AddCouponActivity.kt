package com.yapp.buddycon.presentation.ui.addCoupon

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.yapp.buddycon.domain.model.AddCouponResult
import com.yapp.buddycon.domain.model.CouponInputInfo
import com.yapp.buddycon.presentation.R
import com.yapp.buddycon.presentation.base.BaseActivity
import com.yapp.buddycon.presentation.databinding.ActivityAddCouponBinding
import com.yapp.buddycon.presentation.ui.addCoupon.state.AddCouponResultState
import com.yapp.buddycon.presentation.ui.addCoupon.state.ContentInputState
import com.yapp.buddycon.presentation.ui.addCoupon.state.CouponInfoLoadState
import com.yapp.buddycon.presentation.ui.addCoupon.state.WhetherInputPossibleState
import com.yapp.buddycon.presentation.utils.Logging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddCouponActivity : BaseActivity<ActivityAddCouponBinding>(R.layout.activity_add_coupon) {
    private val TAG = "AppTest"
    private val addCouponViewModel: AddCouponViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.vm = addCouponViewModel

        init()
        observeCouponInfoState()
        observeCouponInputState()
        observeAddCouponResultState()
    }

    private fun init() {
        initAppbar()
        getBarcodeNumber()
        initTitleHint()
        initTextWatcher()
    }

    private fun getBarcodeNumber() {
        val imageUri = intent.getParcelableExtra<Uri>("imageUri")
        Logging.error("AddCouponActivity/ imgUri : $imageUri")
        binding.shivCoupon.setImageURI(imageUri)// ?????? ?????????????????? ???????????? ????????? uri??? ??????????????? ???????????? ??????

        imageUri?.let { uri ->
            val barcodeScanner = BarcodeScanning.getClient()
            val inputImage = InputImage.fromFilePath(this, uri)

            addCouponViewModel.setImageUri(absolutelyPath(uri, this)) // image uri ?????? ?????? ??? ??????

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    Logging.error("barcode list size : ${barcodes.size}")

                    if (barcodes.size >= 1) {
                        barcodes[0]?.let { barcode ->
                            val barcodeNumber = barcode.rawValue
                            Logging.error("barcode number : $barcodeNumber")

                            // ????????? ????????? ????????? ????????? ????????????
                            barcodeNumber?.let {
                                addCouponViewModel.setBarcode(it)
                                addCouponViewModel.checkBarcodeInfo(it)
                            }
                        }
                    } else {
                        Logging.error("read image success but no barcode")
                        MessageDialogFragment("????????? ?????? ?????? \n???????????? ?????? ??????????????????") {
                            finish()
                        }.show(supportFragmentManager, null)
                    }
                }.addOnFailureListener {
                    Logging.error("read image fail")
                }
        }
    }

    private fun initAppbar() {
        binding.appbarAddCoupon.ibnAppbarBack.setOnClickListener { finish() }
    }

    private fun setAppbarTitle(title: String) {
        binding.appbarAddCoupon.title = title
    }

    private fun initTitleHint() {
        // ?????? ?????? ????????? ??????
        val builder = SpannableStringBuilder()
        val titleHint = binding.etTitle.hint.toString()
        val titleHintSpannable = SpannableString(titleHint)

        titleHintSpannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.orange100)),
            titleHint.length - 1, titleHint.length, 0
        )

        builder.append(titleHintSpannable)
        binding.etTitle.hint = builder
    }

    private fun observeCouponInfoState() {
        addCouponViewModel.couponInfoLoadState.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                handleCouponInfoLoadState(it)
            }.launchIn(lifecycleScope)
    }

    // ???????????? ?????? ?????? ex> loading progress bar
    private fun handleCouponInfoLoadState(state: CouponInfoLoadState<CouponInputInfo>) {
        when (state) {
            is CouponInfoLoadState.Init -> {}
            is CouponInfoLoadState.ShowLoading -> {
                Logging.error("AddCouponActivity : show loading")
                binding.pbLoading.isVisible = true
                this.getWindow()?.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
            }
            is CouponInfoLoadState.HideLoading -> {
                Logging.error("AddCouponActivity : hide loading")
                binding.pbLoading.isVisible = false
                this.getWindow()?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
            is CouponInfoLoadState.LoadError -> {
                Toast.makeText(this, "???????????? ????????? ???????????? ???????????? ????????? ??????????????????", Toast.LENGTH_SHORT).show()
                finish()
                // ?????? ?????? ????????????
            }
            is CouponInfoLoadState.NewGifticon -> {
                Logging.error("AddCouponActivity : ?????? ??????")
                setAppbarTitle(getString(R.string.add_coupon_app_bar_title_gifticon))
                setContentInputType(WhetherInputPossibleState.Possible)
                setSentMemberVisibility(state)
            }
            is CouponInfoLoadState.ExistGifticon -> {
                Logging.error("AddCouponActivity : ???????????? ????????????")
                setAppbarTitle(getString(R.string.add_coupon_app_bar_title_gifticon))
                setContentInputType(WhetherInputPossibleState.Impossible)
                setSentMemberVisibility(state)
            }
            is CouponInfoLoadState.ExistMakeCon -> {
                Logging.error("AddCouponActivity : ???????????? ????????????")
                setAppbarTitle(getString(R.string.add_coupon_app_bar_title_makecon))
                setContentInputType(WhetherInputPossibleState.Impossible)
                setSentMemberVisibility(state)
            }
        }
    }

    private fun observeCouponInputState() {
        addCouponViewModel.contentInputState.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                handleCouponInputState(it)
            }.launchIn(lifecycleScope)
    }

    // string resource ?????? ??????
    private fun handleCouponInputState(contentInputState: ContentInputState) {
        when (contentInputState) {
            is ContentInputState.EmptyTitle -> {
                MessageDialogFragment("?????? ????????? ????????? ?????????") {}
                    .show(supportFragmentManager, null)
            }
            is ContentInputState.OutOfRangeTitle -> {
                MessageDialogFragment("?????? ????????? ?????? 16??????\n????????? ??? ?????????") {}
                    .show(supportFragmentManager, null)
            }
            is ContentInputState.EmptyExpireDate -> {
                MessageDialogFragment("??????????????? ????????? ?????????") {}
                    .show(supportFragmentManager, null)
            }
            is ContentInputState.OutOfRangeStoreName -> {
                MessageDialogFragment("???????????? ?????? 16??????\n????????? ??? ?????????") {}
                    .show(supportFragmentManager, null)
            }
            is ContentInputState.OutOfRangeMemo -> {
                MessageDialogFragment("????????? ?????? 50??????\n????????? ??? ?????????") {}
                    .show(supportFragmentManager, null)
            }
        }
    }

    private fun observeAddCouponResultState() {
        addCouponViewModel.addCouponState.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                handleAddCouponResultState(it)
            }.launchIn(lifecycleScope)
    }

    private fun handleAddCouponResultState(resultState: AddCouponResultState<AddCouponResult>) {
        when (resultState) {
            is AddCouponResultState.Init -> {}
            is AddCouponResultState.ShowLoading -> {
                Logging.error("AddCouponActivity : show loading")
                binding.pbLoading.isVisible = true
                this.getWindow()?.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
            }
            is AddCouponResultState.HideLoading -> {
                Logging.error("AddCouponActivity : hide loading")
                binding.pbLoading.isVisible = false
                this.getWindow()?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
            is AddCouponResultState.Success -> {
                Logging.error("AddCouponActivity : add coupon Success")
                MessageDialogFragment(getString(R.string.add_coupon_success_message)) { finish() }
                    .show(supportFragmentManager, null)
            }
            is AddCouponResultState.Error -> {
                Logging.error("AddCouponActivity : add coupon Error")
                Snackbar.make(
                    binding.rootCl,
                    getString(R.string.add_coupon_fail_message),
                    Snackbar.LENGTH_SHORT
                ).apply {
                    anchorView = binding.tvCancel
                }.show()
            }
        }
    }

    // ?????? ?????? ??? ????????????????????? ?????? ??????
    // ?????? ???????????? ??????????????? ?????? ???????????? ?????? ????????? ??? ??? ????????? ???H
    private fun setContentInputType(whetherInputPossibleState: WhetherInputPossibleState) {
        when (whetherInputPossibleState) {
            is WhetherInputPossibleState.Possible -> {
                with(binding) {
                    etTitle.isEnabled = true
                    etStoreName.isEnabled = true
                    etSentMember.isEnabled = true
                }
            }
            is WhetherInputPossibleState.Impossible -> {
                with(binding) {
                    etTitle.isEnabled = false
                    etStoreName.isEnabled = false
                    etSentMember.isEnabled = false
                }
            }
        }
    }

    // ??????????????? ???????????? '????????????' ?????? ??????
    private fun setSentMemberVisibility(couponInfoState: CouponInfoLoadState<CouponInputInfo>) {
        with(binding) {
            etSentMember.isVisible =
                couponInfoState is CouponInfoLoadState.ExistMakeCon<CouponInputInfo>
            tvSentMemberDescription.isVisible =
                couponInfoState is CouponInfoLoadState.ExistMakeCon<CouponInputInfo>
            viewDivider3.isVisible =
                couponInfoState is CouponInfoLoadState.ExistMakeCon<CouponInputInfo>
        }
    }

    fun onClickAddCouponExpireDate(view: View) {
        Logging.error("onClickAddCouponExpireDate called")

        // date picker
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.custom_date_picker_style)
            .build()

        datePicker.addOnPositiveButtonClickListener { longValue ->
            Logging.error("selected date's Long value : ${longValue}")
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"), Locale.KOREAN)
            calendar.timeInMillis = longValue

            val selectedDate = calendar.time
            val dateFormatForUser = SimpleDateFormat("yyyy??? MM??? dd???")
            val dateFormatForServer = SimpleDateFormat("yyyy-MM-dd")

            val selectedDateStringForUser = dateFormatForUser.format(selectedDate)
            val selectedDateStringForServer = dateFormatForServer.format(selectedDate)

            Logging.error("selected date string : $selectedDateStringForUser")
            Logging.error("selected date string for server : $selectedDateStringForServer")

            addCouponViewModel.setExpireDate(selectedDateStringForServer)
            with(binding.tvExpireDate) {
                text = selectedDateStringForUser
                setTextColor(ContextCompat.getColor(this@AddCouponActivity, R.color.gray90))
            } // ?????? ??????
        }

        datePicker.show(supportFragmentManager, "date_picker")
        // Activity ??? theme??? MaterialComponent ????????? ?????? ?????? >> Manifest ?????? ???????????????!!
    }

    fun onClickAddCouponCanel(view: View) {
        TwoOptionTypeDialogFragment(
            getString(R.string.add_coupon_cancel_message),
            "????????????",
            "?????????",
            {},
            { finish() }
        ).show(supportFragmentManager, null)
    }

    private fun initTextWatcher() {
        initTitleTextWatcher()
        initStoreNameTextWatcher()
        initSentMemberTextWatcher()
        initMemoTextWatcher()
    }

    private fun initTitleTextWatcher() {
        binding.etTitle.addTextChangedListener {
            addCouponViewModel.setTitle(it.toString())
        }
    }

    private fun initStoreNameTextWatcher() {
        binding.etStoreName.addTextChangedListener {
            addCouponViewModel.setStoreName(it.toString())
        }
    }

    private fun initSentMemberTextWatcher() {
        binding.etSentMember.addTextChangedListener {
            addCouponViewModel.setSentMemberName(it.toString())
        }
    }

    private fun initMemoTextWatcher() {
        binding.etMemo.addTextChangedListener {
            addCouponViewModel.setMemo(it.toString())
        }
        // ?????? ?????? <- afterTextChanged ??? ?????? ?????????????????? ??????
    }

    // ???????????? ??????
    fun absolutelyPath(path: Uri?, context: Context): String? {
        val proj: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        val c: Cursor? = context.contentResolver.query(path!!, proj, null, null, null)
        val index = c?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        c?.moveToFirst()

        val resultPath = c?.getString(index!!)
        c?.close()

        return resultPath
    }

    override fun onBackPressed() {
        onClickAddCouponCanel(binding.root)
    }
}