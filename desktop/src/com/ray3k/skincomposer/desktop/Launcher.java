/*
 * The MIT License
 *
 * Copyright 2019 Raymond Buckley.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ray3k.skincomposer.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.ray3k.skincomposer.CloseListener;
import com.ray3k.skincomposer.DesktopWorker;
import com.ray3k.skincomposer.FilesDroppedListener;
import com.ray3k.skincomposer.Main;
import com.ray3k.skincomposer.TextFileApplicationLogger;
import com.ray3k.skincomposer.utils.Utils;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 *
 * @author Raymond
 */
public class Launcher implements DesktopWorker, Lwjgl3WindowListener {
    private Array<FilesDroppedListener> filesDroppedListeners;
    private CloseListener closeListener;

    public Launcher() {
        filesDroppedListeners = new Array<>();
    }
    
    @Override
    public void texturePack(Array<FileHandle> handles, FileHandle localFile, FileHandle targetFile, FileHandle settingsFile) {
        Json json = new Json();
        TexturePacker.Settings settings = json.fromJson(TexturePacker.Settings.class, settingsFile);
        
        TexturePacker p = new TexturePacker(settings);
        for (FileHandle handle : handles) {
            if (handle.exists()) {
                p.addImage(handle.file());
            } else {
                if (localFile != null) {
                    FileHandle localHandle = localFile.sibling(localFile.nameWithoutExtension() + "_data/" + handle.name());
                    if (localHandle.exists()) {
                        p.addImage(localHandle.file());
                    } else {
                        Gdx.app.error(getClass().getName(), "File does not exist error while creating texture atlas: " + handle.path());
                    }
                } else {
                    Gdx.app.error(getClass().getName(), "File does not exist error while creating texture atlas: " + handle.path());
                }
            }
        }
        p.pack(targetFile.parent().file(), targetFile.nameWithoutExtension());
    }

    @Override
    public void packFontImages(Array<FileHandle> files, FileHandle saveFile) {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.pot = false;
        settings.duplicatePadding = true;
        settings.filterMin = Texture.TextureFilter.Linear;
        settings.filterMag = Texture.TextureFilter.Linear;
        settings.ignoreBlankImages = false;
        settings.useIndexes = false;
        settings.limitMemory = false;
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        settings.flattenPaths = true;
        settings.silent = true;
        TexturePacker texturePacker = new TexturePacker(settings);

        for (FileHandle file : files) {
            if (file.exists()) {
                texturePacker.addImage(file.file());
            }
        }

        texturePacker.pack(saveFile.parent().file(), saveFile.nameWithoutExtension());
    }
    
    @Override
    public void centerWindow(Graphics graphics) {
        Lwjgl3Graphics g = (Lwjgl3Graphics) graphics;
        Graphics.DisplayMode mode = g.getDisplayMode();
        Lwjgl3Window window = g.getWindow();
        window.setPosition(mode.width / 2 - g.getWidth() / 2, mode.height / 2 - g.getHeight() / 2);
    }

    @Override
    public void sizeWindowToFit(int maxWidth, int maxHeight, int displayBorder, Graphics graphics) {
        Graphics.DisplayMode mode = graphics.getDisplayMode();
        
        int width = Math.min(mode.width - displayBorder * 2, maxWidth);
        int height = Math.min(mode.height - displayBorder * 2, maxHeight);
        
        graphics.setWindowedMode(width, height);
        
        centerWindow(graphics);
    }

    @Override
    public void iconified(boolean isIconified) {
        
    }

    @Override
    public void focusLost() {
        
    }

    @Override
    public void focusGained() {
        
    }

    @Override
    public boolean closeRequested() {
        if (closeListener != null) {
            return closeListener.closed();
        } else {
            return true;
        }
    }
    
    @Override
    public void addFilesDroppedListener(FilesDroppedListener filesDroppedListener) {
        filesDroppedListeners.add(filesDroppedListener);
    }
    
    @Override
    public void removeFilesDroppedListener(FilesDroppedListener filesDroppedListener) {
        filesDroppedListeners.removeValue(filesDroppedListener, false);
    }
    
    @Override
    public void filesDropped(String[] files) {
        Array<FileHandle> fileHandles = new Array<>();
        for (String file : files) {
            FileHandle fileHandle = new FileHandle(file);
            fileHandles.add(fileHandle);
        }
        
        for (FilesDroppedListener listener : filesDroppedListeners) {
            listener.filesDropped(fileHandles);
        }
    }

    @Override
    public void setCloseListener(CloseListener closeListener) {
        this.closeListener = closeListener;
    }

    @Override
    public void attachLogListener() {
        ((Lwjgl3Application) Gdx.app).setApplicationLogger(new TextFileApplicationLogger());
    }

    @Override
    public void maximized(boolean arg0) {
    }

    @Override
    public void refreshRequested() {
    }

    @Override
    public List<File> openMultipleDialog(String title, String defaultPath,
            String[] filterPatterns, String filterDescription) {
        String result = null;
        
        //fix file path characters
        if (Utils.isWindows()) {
            defaultPath = defaultPath.replace("/", "\\");
        } else {
            defaultPath = defaultPath.replace("\\", "/");
        }
        if (filterPatterns != null && filterPatterns.length > 0) {
            try (MemoryStack stack = stackPush()) {
                PointerBuffer pointerBuffer = stack.mallocPointer(filterPatterns.length);

                for (String filterPattern : filterPatterns) {
                    pointerBuffer.put(stack.UTF8(filterPattern));
                }
                
                pointerBuffer.flip();
                result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(title, defaultPath, pointerBuffer, filterDescription, true);
            }
        } else {
            result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(title, defaultPath, null, filterDescription, true);
        }
        
        if (result != null) {
            String[] paths = result.split("\\|");
            ArrayList<File> returnValue = new ArrayList<>();
            for (String path : paths) {
                returnValue.add(new File(path));
            }
            return returnValue;
        } else {
            return null;
        }
    }
    
    @Override
    public File openDialog(String title, String defaultPath,
            String[] filterPatterns, String filterDescription) {
        String result = null;
        
        //fix file path characters
        if (Utils.isWindows()) {
            defaultPath = defaultPath.replace("/", "\\");
        } else {
            defaultPath = defaultPath.replace("\\", "/");
        }
        
        if (filterPatterns != null && filterPatterns.length > 0) {
            try (MemoryStack stack = stackPush()) {
                PointerBuffer pointerBuffer = stack.mallocPointer(filterPatterns.length);

                for (String filterPattern : filterPatterns) {
                    pointerBuffer.put(stack.UTF8(filterPattern));
                }
                
                pointerBuffer.flip();
                result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(title, defaultPath, pointerBuffer, filterDescription, false);
            }
        } else {
            result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(title, defaultPath, null, filterDescription, false);
        }
        
        if (result != null) {
            return new File(result);
        } else {
            return null;
        }
    }
    
    @Override
    public File saveDialog(String title, String defaultPath,
            String[] filterPatterns, String filterDescription) {
        String result = null;
        
        //fix file path characters
        if (Utils.isWindows()) {
            defaultPath = defaultPath.replace("/", "\\");
        } else {
            defaultPath = defaultPath.replace("\\", "/");
        }
        
        if (filterPatterns != null && filterPatterns.length > 0) {
            try (MemoryStack stack = stackPush()) {
                PointerBuffer pointerBuffer = null;
                pointerBuffer = stack.mallocPointer(filterPatterns.length);

                for (String filterPattern : filterPatterns) {
                    pointerBuffer.put(stack.UTF8(filterPattern));
                }
                
                pointerBuffer.flip();
                result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_saveFileDialog(title, defaultPath, pointerBuffer, filterDescription);
            }
        } else {
            result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_saveFileDialog(title, defaultPath, null, filterDescription);
        }
        
        if (result != null) {
            return new File(result);
        } else {
            return null;
        }
    }

    @Override
    public void closeSplashScreen() {
    }

    @Override
    public char getKeyName(int keyCode) {
        int glfwKeyCode = Lwjgl3Input.getGlfwKeyCode(keyCode);
        try {
            String output = org.lwjgl.glfw.GLFW.glfwGetKeyName(glfwKeyCode, 0);
            return (output == null) ? ' ' : output.toLowerCase().charAt(0);
        } catch (Exception e) {
            return ' ';
        }
    }

    @Override
    public void writeFont(FreeTypeFontGenerator.FreeTypeBitmapFontData data, Array<PixmapPacker.Page> pages, FileHandle target) {
        FileHandle pngTarget = target.sibling(target.nameWithoutExtension() + ".png");
        
        BitmapFontWriter.FontInfo info = new BitmapFontWriter.FontInfo();
        data.capHeight--;
        info.face = target.nameWithoutExtension();
        info.padding = new BitmapFontWriter.Padding(1, 1, 1, 1);

        BitmapFontWriter.writePixmaps(pages, target.parent(), target.nameWithoutExtension());
        
        Pixmap pixmap = new Pixmap(pngTarget);
        Color color = new Color();
        int newHeight = pixmap.getHeight();
        boolean foundOpaquePixel = false;
        for (int y = pixmap.getHeight() - 1; y >= 0 && !foundOpaquePixel; y--) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                color.set(pixmap.getPixel(x, y));
                if (color.a > 0) {
                    //add padding to new height
                    newHeight = y + 2;
                    foundOpaquePixel = true;
                    break;
                }
            }
        }
        
        foundOpaquePixel = false;
        int newWidth = pixmap.getWidth();
        for (int x = pixmap.getWidth() - 1; x >= 0 && !foundOpaquePixel; x--) {
            for (int y = 0; y < pixmap.getHeight(); y++) {
                color.set(pixmap.getPixel(x, y));
                if (color.a > 0) {
                    //add padding to new height
                    newWidth = x + 2;
                    foundOpaquePixel = true;
                    break;
                }
            }
        }
        
        Pixmap fixedPixmap = new Pixmap(newWidth, newHeight, Pixmap.Format.RGBA8888);
        fixedPixmap.setBlending(Pixmap.Blending.None);
        fixedPixmap.drawPixmap(pixmap, 0, 0);
        PixmapIO.writePNG(pngTarget, fixedPixmap);
        
        BitmapFontWriter.writeFont(data, new String[]{target.nameWithoutExtension() + ".png"}, target, info, newWidth, newHeight);
        pixmap.dispose();
        fixedPixmap.dispose();
    }

    @Override
    public void created(Lwjgl3Window lw) {
        
    }
}
