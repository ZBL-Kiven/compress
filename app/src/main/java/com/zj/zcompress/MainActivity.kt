package com.zj.zcompress

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.zj.album.AlbumIns
import com.zj.album.nModule.FileInfo
import com.zj.compress.CompressUtils
import com.zj.compress.videos.FileUtils
import com.zj.compress.videos.CompressListener
import java.io.File


@SuppressLint("SetTextI18n")
class MainActivity : FragmentActivity() {

    private var textView: TextView? = null
    private var tvTime: TextView? = null
    private var tvType: TextView? = null
    private var tvStartSize: TextView? = null
    private var tvProgress: TextView? = null
    private var tvEndSize: TextView? = null
    private lateinit var iv1: ImageView
    private lateinit var iv2: ImageView
    private lateinit var iv3: ImageView
    private var fileInfo: FileInfo? = null
    private var startTime: Long = 0
    private val fileSelectorCode = 0x1212

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.compresse_tv)
        tvProgress = findViewById(R.id.tv_progress)
        tvType = findViewById(R.id.tv_type)
        tvTime = findViewById(R.id.tv_time)
        tvStartSize = findViewById(R.id.tv_startSize)
        tvEndSize = findViewById(R.id.tv_endSize)
        iv1 = findViewById(R.id.iv1)
        iv2 = findViewById(R.id.iv2)
        iv3 = findViewById(R.id.iv3)

        findViewById<View>(R.id.btn_choose).setOnClickListener {

            //                        startAlbum()

            openFileSelector()
        }

        findViewById<View>(R.id.btn_start).setOnClickListener {
            startTime = System.currentTimeMillis()
            fileInfo?.let {
                when {
                    it.isImage -> startCompressImage(Uri.parse(it.path))
                    it.isVideo -> startCompressVideo(Uri.parse(it.path))
                }
            } ?: Toast.makeText(this, "please select a video first", Toast.LENGTH_SHORT).show()
        }


    }

    private fun openFileSelector() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, fileSelectorCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileSelectorCode) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data?.data
                transaction(uri)
            }
        }
    }

    private fun transaction(uri: Uri?) {
        CompressUtils.with(this).load(uri).transForAndroidQ {
            if (it?.path != null) {
                Log.e("------- ", "${File(it.path ?: "").exists()}")
            }
        }
    }

    private fun startCompressImage(uri: Uri?) {
        CompressUtils.with(this).load(uri).asImage().ignoreBy(1024).setQuality(80).start(object : com.zj.compress.images.CompressListener {

            override fun onFileTransform(info: com.zj.compress.FileInfo.ImageFileInfo?, compressEnable: Boolean) {
                Glide.with(this@MainActivity).load(info?.path).into(this@MainActivity.iv2)
                runOnUiThread { tvStartSize?.text = "压缩前大小 ： ${FileUtils.getFormatSize(info?.path)}  /  ${info?.width}X${info?.height}" }
                Log.e("------ ", "file transfer ==> ${info?.path}")
            }

            override fun onStart() {
                Log.e("------ ", "onStart ==> ${uri.toString()}")
            }

            override fun onSuccess(path: String?) {
                path?.let {
                    Glide.with(this@MainActivity).load(it).into(this@MainActivity.iv3)
                    tvTime?.text = "压缩用时 ：${((System.currentTimeMillis() - startTime) / 1000f * 100f).toInt() / 100f} 秒"
                    tvEndSize?.text = "压缩后大小 ： ${FileUtils.getFormatSize(it)}"
                    Log.e("------ ", "onSuccess ==> $path")
                }
            }

            override fun onError(code: Int, e: Throwable?) {
                tvEndSize?.text = "压缩失败 ,case: ${e?.message}"
            }
        })
    }

    private fun startCompressVideo(uri: Uri) {
        CompressUtils.with(this).load(uri).asVideo().setLevel(1600).setOutPutFileName("/file/${System.currentTimeMillis()}.mp4").start(object : CompressListener {

            override fun onSuccess(var1: String?) {
                var1?.let {
                    Glide.with(this@MainActivity).load(it).into(this@MainActivity.iv3)
                    tvTime?.text = "压缩用时 ：${((System.currentTimeMillis() - startTime) / 1000f * 100f).toInt() / 100f} 秒"
                    tvEndSize?.text = "压缩后大小 ： ${FileUtils.getFormatSize(it)}"
                }
            }

            override fun onCancel() {
                tvEndSize?.text = "已取消"
            }

            override fun onProgress(var1: Float) {
                tvProgress?.text = "压缩进度 ：$var1"
            }

            override fun onError(var1: Int, s: String) {
                tvEndSize?.text = "压缩失败 ,case: $s"
            }

            override fun onFileTransform(info: com.zj.compress.FileInfo.VideoFileInfo?, compressEnable: Boolean) {
                Glide.with(this@MainActivity).load(fileInfo?.path).into(this@MainActivity.iv2)
                runOnUiThread { tvStartSize?.text = "压缩前大小 ： ${FileUtils.getFormatSize(fileInfo?.path)}  /  ${info?.width}X${info?.height}" }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        start()
    }

    private fun start() {
        AlbumIns.with(this).setOriginalPolymorphism(true).simultaneousSelection(true).maxSelectedCount(1).start { _, data ->
            fileInfo = data?.get(0)
            tvType?.text = fileInfo?.mimeType
            tvStartSize?.text = ""
            tvEndSize?.text = ""
            tvProgress?.text = ""
            tvTime?.text = ""
            Glide.with(this@MainActivity).load("file://" + fileInfo?.path).into(this@MainActivity.iv1)
        }
    }

    private fun startAlbum() {
        val i = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val t = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (i == PackageManager.PERMISSION_GRANTED && t == PackageManager.PERMISSION_GRANTED) {
            start()
        } else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
    }
}