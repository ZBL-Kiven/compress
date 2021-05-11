package com.zj.compress

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.zj.album.AlbumIns
import com.zj.album.options.AlbumOptions
import java.io.File

@SuppressLint("SetTextI18n")
class MainActivity : FragmentActivity() {

    private var textView: TextView? = null
    private var tvTime: TextView? = null
    private var tvStartSize: TextView? = null
    private var tvProgress: TextView? = null
    private var tvEndSize: TextView? = null
    private var path: String = ""
    private var contentUri: Uri? = null
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.compresse_tv)
        tvProgress = findViewById(R.id.tv_progress)
        tvTime = findViewById(R.id.tv_time)
        tvStartSize = findViewById(R.id.tv_startSize)
        tvEndSize = findViewById(R.id.tv_endSize)

        findViewById<View>(R.id.btn_choose).setOnClickListener {
            startAlbum()
        }

        findViewById<View>(R.id.btn_start).setOnClickListener {
            if (contentUri == null || path.isEmpty()) {
                Toast.makeText(this, "please select a video first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startCompress(contentUri!!) else startCompress(Uri.parse(path))
        }
    }

    private fun startCompress(uri: Uri) {
        startTime = System.currentTimeMillis()
        VideoCompressUtils.create(this.application).setInputFilePath(uri).setOutPutFileName("/file/${System.currentTimeMillis()}.mp4").setLevel(1600).build().start(object : CompressListener {

            override fun onSuccess(var1: String?) {
                var1?.let {
                    tvTime?.text = "压缩用时 ：${((System.currentTimeMillis() - startTime) / 1200f).toInt()} 秒"
                    tvEndSize?.text = "压缩后大小 ： ${FileUtils.getFormatSize(it)}"
                    File(it).delete()
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

            override fun onFilePatched(path: String?): Boolean {
                runOnUiThread { tvStartSize?.text = "压缩前大小 ： ${FileUtils.getFormatSize(path)}" }
                return true
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        start()
    }

    private fun start() {
        AlbumIns.with(this).setOriginalPolymorphism(true).simultaneousSelection(true).maxSelectedCount(1).mimeTypes(AlbumOptions.pairOf(AlbumOptions.ofVideo())).videoSizeRange(100 * 1024, 200 * 1024 * 1024).start { _, data ->
            path = data?.get(0)?.path ?: ""
            contentUri = data?.get(0)?.getContentUri()
            tvStartSize?.text = ""
            tvEndSize?.text = ""
            tvProgress?.text = ""
            tvTime?.text = ""
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