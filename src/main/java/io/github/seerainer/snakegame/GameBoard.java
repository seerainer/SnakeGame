package io.github.seerainer.snakegame;

import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;

import java.security.SecureRandom;
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

class GameBoard extends Canvas {

	private enum Direction {
		UP, DOWN, LEFT, RIGHT
	}

	private static final int CELL_SIZE = 20;
	private static int GAME_SPEED = 150; // Lower is faster

	private boolean gameInitialized;
	private boolean gameOver;
	private int boardWidth;
	private int boardHeight;
	private int score;

	private Direction currentDirection;
	private final LinkedList<Point> snake;
	private Point food;
	private Runnable gameLoop;
	private final SecureRandom random;

	GameBoard(final Composite parent) {
		super(parent, SWT.DOUBLE_BUFFERED);

		random = new SecureRandom();
		snake = new LinkedList<>();
		currentDirection = Direction.RIGHT;
		gameOver = false;
		score = 0;
		gameInitialized = false;

		addPaintListener(e -> {
			initGame();
			drawGame(e.gc);
		});

		addKeyListener(keyPressedAdapter(this::handleKeyPress));

		// Start the game after a short delay to ensure canvas is ready
		getDisplay().asyncExec(() -> {
			initGame();
			startGameLoop();
		});
	}

	private void drawGame(final GC gc) {
		final var display = getDisplay();

		// Clear the canvas
		gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(0, 0, getSize().x, getSize().y);

		// Draw the snake
		gc.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
		snake.forEach((final Point segment) -> gc.fillRectangle(segment.x * CELL_SIZE, segment.y * CELL_SIZE, CELL_SIZE,
				CELL_SIZE));

		// Draw the food - added null check
		if (food != null) {
			gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
			gc.fillOval(food.x * CELL_SIZE, food.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
		}

		// Update the shell title with current game speed and score
		getShell().setText("Snake Game - Speed: " + GAME_SPEED + "ms, Score: " + score);

		// Draw game over message
		if (!gameOver) {
			return;
		}
		final var message = "Game Over!\nPress [ESC] to restart.\nPress [P] to pause.\nPress [Q] to quit.";
		gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
		gc.drawText(message, (getSize().x - gc.textExtent(message).x) / 2, getSize().y / 2, true);
	}

	private void handleKeyPress(final KeyEvent e) {
		switch (e.keyCode) {
		case SWT.ARROW_UP -> {
			if (currentDirection != Direction.DOWN) {
				currentDirection = Direction.UP;
			}
		}
		case SWT.ARROW_DOWN -> {
			if (currentDirection != Direction.UP) {
				currentDirection = Direction.DOWN;
			}
		}
		case SWT.ARROW_LEFT -> {
			if (currentDirection != Direction.RIGHT) {
				currentDirection = Direction.LEFT;
			}
		}
		case SWT.ARROW_RIGHT -> {
			if (currentDirection != Direction.LEFT) {
				currentDirection = Direction.RIGHT;
			}
		}
		case SWT.ESC -> {
			final var wasGameOver = gameOver; // Capture the state before reset
			gameInitialized = false; // Reset game initialization state
			initGame(); // Reset the game
			if (wasGameOver) {
				startGameLoop(); // Restart the game loop if it was stopped
			}
		}
		default -> {
			if (e.character == 'q' || e.character == 'Q') {
				getShell().close(); // Close the game on 'q' or 'Q'
			} else if (e.character == 'p' || e.character == 'P') {
				if (gameLoop != null) {
					getDisplay().timerExec(-1, gameLoop); // Pause the game
					gameLoop = null;
				} else {
					startGameLoop(); // Resume the game
				}
			}
		}
		}
	}

	private void initGame() {
		if (gameInitialized) {
			return;
		}
		boardWidth = Math.max(10, getSize().x / CELL_SIZE); // Ensure minimum size
		boardHeight = Math.max(10, getSize().y / CELL_SIZE);

		// Create initial snake (3 segments)
		snake.clear();
		final var centerX = boardWidth / 2;
		final var centerY = boardHeight / 2;
		snake.add(new Point(centerX, centerY));
		snake.add(new Point(centerX - 1, centerY));
		snake.add(new Point(centerX - 2, centerY));

		// Create initial food
		placeFood();

		// Reset game state
		currentDirection = Direction.RIGHT;
		gameInitialized = true;
		gameOver = false;
		score = 0;
		GAME_SPEED = 150; // Reset speed to initial value
	}

	private void moveSnake() {
		final var head = snake.getFirst();
		final var newHead = new Point(head.x, head.y);

		// Move the head according to current direction
		switch (currentDirection) {
		case UP -> newHead.y--;
		case DOWN -> newHead.y++;
		case LEFT -> newHead.x--;
		case RIGHT -> newHead.x++;
		default -> {
			return;
		}
		}

		// Handle wrap-around for walls
		if (newHead.x < 0) {
			newHead.x = boardWidth - 1; // Wrap to right side
		} else if (newHead.x >= boardWidth) {
			newHead.x = 0; // Wrap to left side
		}

		if (newHead.y < 0) {
			newHead.y = boardHeight - 1; // Wrap to bottom
		} else if (newHead.y >= boardHeight) {
			newHead.y = 0; // Wrap to top
		}

		// Check for self-collision before adding new head
		for (final var segment : snake) {
			if (newHead.x == segment.x && newHead.y == segment.y) {
				gameOver = true;
				return;
			}
		}

		snake.addFirst(newHead);

		// Remove tail if no food is eaten
		// Added null check for food
		if (food != null && !newHead.equals(food)) {
			snake.removeLast();
		} else if (food != null) {
			score++;
			placeFood();
			if (score % 10 == 0) {
				GAME_SPEED = Math.max(50, GAME_SPEED - 2); // Minimum speed limit of 50ms
			}
		}
	}

	private void placeFood() {
		// Guard against zero board dimensions
		if (boardWidth <= 0 || boardHeight <= 0) {
			boardWidth = Math.max(10, getSize().x / CELL_SIZE);
			boardHeight = Math.max(10, getSize().y / CELL_SIZE);
		}

		int x;
		int y;
		boolean validPosition;

		do {
			validPosition = true;
			x = random.nextInt(boardWidth);
			y = random.nextInt(boardHeight);

			// Check if food is not on snake
			for (final var segment : snake) {
				if (segment.x == x && segment.y == y) {
					validPosition = false;
					break;
				}
			}
		} while (!validPosition);

		food = new Point(x, y);
	}

	private void startGameLoop() {
		if (gameLoop != null) {
			getDisplay().timerExec(-1, gameLoop);
		}

		gameLoop = () -> {
			if (isDisposed() || gameOver) {
				return;
			}
			moveSnake();
			redraw();
			getDisplay().timerExec(GAME_SPEED, gameLoop);
		};

		getDisplay().timerExec(GAME_SPEED, gameLoop);
		setFocus(); // Make sure the canvas can receive key events
	}
}
