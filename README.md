# VideoTimelineView
A TimelineView implementation for video cut and trim operation

![Preview](https://github.com/cse-ariful/VideoTimelineView/blob/master/preview.gif?raw=true)


#Usage

1. Add the following line to you project level build.gradle file
   no need if already exist
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
 ```
 
 2.Add this to you app level build.gradle file
 
 ```
 dependencies {
	        implementation 'com.github.cse-ariful:VideoTimelineView:v.1.0.0'
	}
 ```
 
 3.Add following to your xml file
 
 ```
   <com.ariful.mobile.timeline.TimelineView
        android:id="@+id/timeLineView"
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:layout_marginHorizontal="16dp"
        android:layout_marginTop="16dp"
        android:background="@drawable/rounded_corner_4dp" />
```

4. Configure the view in java or kotlin class
```
  
        timeline = findViewById(R.id.timeLineView)
        timeline.setVideoUri(uri)
        
        //needed below line to properly pass the seek duration or if you dont set it you will get percent value
        //the seekMillis value will be icorrect in the callback
       
       timeline.setTotalDuration(player.duration)
        
        timeline.callback = object : TimelineView.Callback{
            override fun onSeek(position: Float, seekMillis: Long) {
                 
            }

            override fun onSeekStart(position: Float, seekMillis: Long) {
             }

            override fun onStopSeek(position: Float, seekMillis: Long) {
             }

            override fun onLeftProgress(leftPos: Float, seekMillis: Long) {
             }

            override fun onRightProgress(rightPos: Float, seekMillis: Long) {
             }
        }

```

#Optional

if you want to show the video play progress in timeline than you have to update the progress from player while playing 
for example if we are using exoplayer we can do the following 

```
private fun initProgress() {
        player.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                mHandler.removeCallbacksAndMessages(null)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    mHandler.sendEmptyMessageDelayed(0, 16)
                } else {
                    mHandler.removeCallbacksAndMessages(null)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                if (state == ExoPlayer.STATE_READY) { 
                    timeline.setTotalDuration(player.duration) 
                }
            }
        })
        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                try {
                    val progress = (player.currentPosition.toFloat() / player.duration.toFloat())
                    timeline.setCurrentProgressValue(progress)
                    // updateProgressDependentComponent(progress)
                } catch (ex: Exception) {
                }
                if (player.isPlaying)
                    mHandler.sendEmptyMessageDelayed(0, 16)
            }
        }
        mHandler.sendEmptyMessage(0)
    }
    ```
 
