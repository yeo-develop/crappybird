package com.yeo.develop.crappybird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrappyBirdGame()
        }
    }
}
@Composable
fun CrappyBirdGame() {
    // 새의 y 좌표
    var birdY by remember { mutableStateOf(0f) }
    // 새의 속도
    var velocity by remember { mutableStateOf(0f) }
    val gravity = 0.5f
    val flapBoost = -10f

    // 파이프 리스트
    var pipes by remember { mutableStateOf(listOf<Pipe>()) }
    // 마지막 파이프 생성 시간
    var lastPipeGenerationTime by remember { mutableStateOf(0L) }
    // 점수
    var score by remember { mutableStateOf(0) }
    // 게임 오버 상태
    var gameOver by remember { mutableStateOf(false) }
    // 화면 높이
    var screenHeight by remember { mutableStateOf(0) }

    // 게임 루프
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frameTime ->
                if (!gameOver) {
                    // 중력에 의해 속도 증가
                    velocity += gravity
                    // 새의 y 좌표 업데이트
                    birdY += velocity

                    // 새가 화면 밖으로 나갔는지 확인
                    if (birdY < 0 || birdY + GameConstants.BIRD_HEIGHT > screenHeight) {
                        gameOver = true
                    }

                    // 파이프 이동 및 다시 생성
                    pipes = pipes.map { pipe ->
                        pipe.copy(x = pipe.x - GameConstants.PIPE_SPEED)
                    }.filter { pipe ->
                        pipe.x + GameConstants.PIPE_WIDTH > 0
                    }

                    // 일정 간격마다 새로운 파이프 생성
                    if (frameTime - lastPipeGenerationTime >= GameConstants.PIPE_GENERATION_INTERVAL * 1_000_000) {
                        pipes = pipes + Pipe(
                            x = 800.0,
                            gapY = Random.nextDouble() * (screenHeight - GameConstants.PIPE_GAP - 200) + 100 // gapY 범위 조정
                        )
                        lastPipeGenerationTime = frameTime
                    }

                    // 충돌 감지
                    pipes.forEach { pipe ->
                        if (pipe.collidesWith(birdY)) {
                            gameOver = true
                        }
                    }

                    // 점수 계산
                    pipes.forEach { pipe ->
                        if (!pipe.passed && pipe.x + GameConstants.PIPE_WIDTH / 2 < GameConstants.BIRD_X) {
                            score += 1
                            pipe.passed = true
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Cyan)
            .onGloballyPositioned { layoutCoordinates ->
                // 화면 높이 저장
                screenHeight = layoutCoordinates.size.height
            },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText("Score: $score", modifier = Modifier.padding(16.dp))

        Canvas(modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .background(Color.Blue)) {
            drawIntoCanvas {
                // 새 그리기
                drawRect(
                    Color.Yellow,
                    size = androidx.compose.ui.geometry.Size(GameConstants.BIRD_WIDTH, GameConstants.BIRD_HEIGHT),
                    topLeft = androidx.compose.ui.geometry.Offset(GameConstants.BIRD_X, birdY)
                )

                // 파이프 그리기
                pipes.forEach { pipe ->
                    // 상단 파이프
                    drawRect(
                        Color.Green,
                        size = androidx.compose.ui.geometry.Size(GameConstants.PIPE_WIDTH, pipe.gapY.toFloat()),
                        topLeft = androidx.compose.ui.geometry.Offset(pipe.x.toFloat(), 0f)
                    )
                    // 하단 파이프
                    val bottomPipeHeight = screenHeight - (pipe.gapY + GameConstants.PIPE_GAP).toFloat()
                    drawRect(
                        Color.Green,
                        size = androidx.compose.ui.geometry.Size(GameConstants.PIPE_WIDTH, bottomPipeHeight),
                        topLeft = androidx.compose.ui.geometry.Offset(pipe.x.toFloat(), (pipe.gapY + GameConstants.PIPE_GAP).toFloat())
                    )
                }
            }
        }

        BasicText(
            text = if (gameOver) "게임오버!!!!!! 다시 클릭하면 재시작해요" else "여길 눌러서 새를 위로 올려요",
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
                .clickable {
                    if (gameOver) {
                        // 게임 오버 후 재시작
                        birdY = 0f
                        velocity = 0f
                        pipes = listOf()
                        score = 0
                        gameOver = false
                    } else {
                        // 새의 속도를 상승으로 설정
                        velocity = flapBoost
                    }
                }
        )
    }
}

object GameConstants {
    const val BIRD_X = 100f
    const val BIRD_WIDTH = 50f
    const val BIRD_HEIGHT = 50f
    const val PIPE_WIDTH = 100f
    const val PIPE_GAP = 200f
    const val PIPE_SPEED = 5f
    const val PIPE_GENERATION_INTERVAL = 2000L // 2 seconds
}

data class Pipe(val x: Double, val gapY: Double) {
    var passed: Boolean = false // 파이프 통과 여부


    /**
     * 새의 위치(birdY)를 받아서 파이프와 충돌했는지 여부를 반환합니다.
     *
     * @param birdY 새의 y 좌표
     * @return 파이프와 충돌했는지 여부
     */
    fun collidesWith(birdY: Float): Boolean {
        val birdX = GameConstants.BIRD_X
        val birdWidth = GameConstants.BIRD_WIDTH
        val birdHeight = GameConstants.BIRD_HEIGHT
        val pipeWidth = GameConstants.PIPE_WIDTH
        val pipeGap = GameConstants.PIPE_GAP

        val birdRight = birdX + birdWidth
        val pipeRight = x + pipeWidth

        // 새가 파이프와 수평으로 겹치는지 확인
        val horizontalCollision = birdRight > x && birdX < pipeRight

        // 새가 파이프의 상단이나 하단 부분과 수직으로 겹치는지 확인
        val verticalCollision = birdY < gapY || birdY + birdHeight > gapY + pipeGap

        return horizontalCollision && verticalCollision
    }
}