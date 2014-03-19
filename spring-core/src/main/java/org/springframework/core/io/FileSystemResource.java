/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Resource} implementation for {@code java.io.File} handles.
 * Obviously supports resolution as File, and also as URL.
 * Implements the extended {@link WritableResource} interface.
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see java.io.File
 */
public class FileSystemResource extends AbstractResource implements WritableResource {

	private final File file;

	private final String path;


	/**
	 * Create a new FileSystemResource from a File handle.
	 * <p>Note: When building relative resources via {@link #createRelative},
	 * the relative path will apply <i>at the same directory level</i>:
	 * e.g. new File("C:/dir1"), relative path "dir2" -> "C:/dir2"!
	 * If you prefer to have relative paths built underneath the given root
	 * directory, use the {@link #FileSystemResource(String) constructor with a file path}
	 * to append a trailing slash to the root path: "C:/dir1/", which
	 * indicates this directory as root for all relative paths.
	 * @param file a File handle
	 */
	public FileSystemResource(File file) {
		Assert.notNull(file, "File must not be null");
		this.file = file;
		this.path = StringUtils.cleanPath(file.getPath());
	}

	/**
	 * Create a new FileSystemResource from a file path.
	 * <p>Note: When building relative resources via {@link #createRelative},
	 * it makes a difference whether the specified resource base path here
	 * ends with a slash or not. In the case of "C:/dir1/", relative paths
	 * will be built underneath that root: e.g. relative path "dir2" ->
	 * "C:/dir1/dir2". In the case of "C:/dir1", relative paths will apply
	 * at the same directory level: relative path "dir2" -> "C:/dir2".
	 * @param path a file path
	 */
	public FileSystemResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.file = new File(path);
		this.path = StringUtils.cleanPath(path);
	}

	/**
	 * Return the file path for this resource.
	 */
	public final String getPath() {
		return this.path;
	}


	/**
	 * This implementation returns whether the underlying file exists.
	 * @see java.io.File#exists()
	 */
	@Override
	public boolean exists() {
		return this.file.exists();
	}

	/**
	 * This implementation checks whether the underlying file is marked as readable
	 * (and corresponds to an actual file with content, not to a directory).
	 * @see java.io.File#canRead()
	 * @see java.io.File#isDirectory()
	 */
	@Override
	public boolean isReadable() {
		return (this.file.canRead() && !this.file.isDirectory());
	}

	/**
	 * <p>zh°很简单的功能，仅仅是：new FileInputStream(this.file); ，可以看出每次得到的值都不相同，所以和其相关的 {@link #isOpen()} 总是 false。</p>
	 * <hr>
	 * <p>en°This implementation opens a FileInputStream for the underlying file.</p>
	 * <p>zh°此实现是打开了一个相关的底层文件（{@link #getFile() this.getFile()}）的 {@link java.io.FileInputStream FileInputStream}。</p>
	 * @see java.io.FileInputStream
	 */
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(this.file);
	}

	/**
	 * <p>zh°很简单的功能，仅仅是：this.file.toURI().toURL(); 。</p>
	 * <hr>
	 * <p>This implementation returns a URL for the underlying file.</p>
	 * <p>此实现是打开一个相关的底层文件（{@link #getFile() this.getFile()}）的 {@link java.net.URL URL}。</p>
	 * @see java.io.File#toURI()
	 */
	@Override
	public URL getURL() throws IOException {
		return this.file.toURI().toURL();
	}

	/**
	 * <p>zh°很简单的功能，仅仅是：this.file.toURI(); 。</p>
	 * <hr>
	 * <p>en°This implementation returns a URI for the underlying file.</p>
	 * <p>zh°此实现是打开一个相关的底层文件（{@link #getFile() this.getFile()}）的 {@link java.net.URI URI}。</p>
	 * @see java.io.File#toURI()
	 */
	@Override
	public URI getURI() throws IOException {
		return this.file.toURI();
	}

	/**
	 * <p>en°This implementation returns the underlying File reference.</p>
	 * <p>zh°此实现是返回相关的底层文件的引用。</p>
	 */
	@Override
	public File getFile() {
		return this.file;
	}

	/**
	 * <p>zh°很简单的功能，仅仅是：this.file.length(); 。</p>
	 * <hr>
	 * <p>en°This implementation returns the underlying File's length.</p>
	 * <p>zh°此实现是返回相关的底层文件的长度。</p>
	 * @see java.io.File#length()
	 */
	@Override
	public long contentLength() throws IOException {
		return this.file.length();
	}

	/**
	 * <p>en°This implementation creates a FileSystemResource, applying the given path
	 * relative to the path of the underlying file of this resource descriptor.</p>
	 * <p>zh°此实现是创建一个 {@link FileSystemResource} 类型的资源。以本资源位置为参考，获取于相对于本资源位置（传递的参数是相对路径）的资源。</p>
	 * @see org.springframework.util.StringUtils#applyRelativePath(String, String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new FileSystemResource(pathToUse);
	}

	/**                                                                                                                         
	 * <p>zh°很简单的功能，仅仅是：this.file.getName(); 。</p>
	 * <hr>
	 * <p>en°This implementation returns the name of the file.</p>
	 * <p>zh°此实现是返回的文件的名称。</p>
	 * @see java.io.File#getName()
	 */
	@Override
	public String getFilename() {
		return this.file.getName();
	}

	/**
	 * <p>zh°很简单的功能，主要是：this.file.getName(); ，在其前后分别增加了“file [”和“]”。</p>
	 * <hr>
	 * <p>en°This implementation returns a description that includes the absolute
	 * path of the file.</p> 
	 * <p>zh°此实现是返回该文件的绝对路径。</p>
	 * @see java.io.File#getAbsolutePath()
	 */
	public String getDescription() {
		return "file [" + this.file.getAbsolutePath() + "]";
	}


	// implementation of WritableResource

	/**
	 * This implementation checks whether the underlying file is marked as writable
	 * (and corresponds to an actual file with content, not to a directory).
	 * @see java.io.File#canWrite()
	 * @see java.io.File#isDirectory()
	 */
	public boolean isWritable() {
		return (this.file.canWrite() && !this.file.isDirectory());
	}

	/**
	 * This implementation opens a FileOutputStream for the underlying file.
	 * @see java.io.FileOutputStream
	 */
	public OutputStream getOutputStream() throws IOException {
		return new FileOutputStream(this.file);
	}


	/**
	 * This implementation compares the underlying File references.
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this ||
			(obj instanceof FileSystemResource && this.path.equals(((FileSystemResource) obj).path)));
	}

	/**
	 * This implementation returns the hash code of the underlying File reference.
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
