package io.github.seerainer.snakegame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Main {
	public static void main(final String[] args) {
		final var display = new Display();
		final var shell = new Shell(display, SWT.SHELL_TRIM & ~SWT.RESIZE & ~SWT.MAX);
		final var gameBoard = new GameBoard(shell);
		shell.setLayout(new FillLayout());
		shell.setSize(500, 500);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		gameBoard.dispose();
		display.dispose();
	}
}