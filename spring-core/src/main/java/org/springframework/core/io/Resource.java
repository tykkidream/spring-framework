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
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * <h3>资源</h3>
 * <p>
 * 是对 Spring 使用的资源的描述，比较底层的封装。
 * </p>
 * <ul>
 * <li>可以表示包括各种类型、各种位置的资源，比如网络位置的、磁盘位置的、内存中的等等。这个性质是由父接口 {@link InputStreamSource} 定义的：是
 * {@link java.io.InputStream InputStream} 类型的资源。</li>
 * <li>包含一些基本的方法，比如资源是否被打开；可以在 {@link java.net.URL URL}、{@link java.net.URI URI}、
 * {@link java.io.File File} 之间转换等。</li>
 * </ul>
 * <p>
 * 有一些实用的实现类：{@link ByteArrayResource}（ Byte 数组资源）、{@link ClassPathResource}（类路径资源）、
 * {@link FileSystemResource}（磁盘文件资源）、{@link InputStreamResource}（ InputStream 资源）、
 * {@link UrlResource}（ URL 资源）。
 * </p>
 * <hr>
 * <p>
 * Interface for a resource descriptor that abstracts from the actual type of underlying
 * resource, such as a file or class path resource.
 * </p>
 * 
 * <p>
 * An InputStream can be opened for every resource if it exists in physical form, but a
 * URL or File handle can just be returned for certain resources. The actual behavior is
 * implementation-specific.
 * 
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see #getInputStream()
 * @see #getURL()
 * @see #getURI()
 * @see #getFile()
 * @see WritableResource
 * @see ContextResource
 * @see FileSystemResource
 * @see ClassPathResource
 * @see UrlResource
 * @see ByteArrayResource
 * @see InputStreamResource
 */
public interface Resource extends InputStreamSource {

	/**
	 * <p>
	 * Return whether this resource actually exists in physical form.
	 * </p>
	 * <p>
	 * 返回这个资源是否实际物理存在。
	 * </p>
	 * <p>
	 * This method performs a definitive existence check, whereas the existence of a
	 * {@code Resource} handle only guarantees a valid descriptor handle.
	 * </p>
	 */
	boolean exists();

	/**
	 * <p>
	 * Return whether the contents of this resource can be read, e.g. via
	 * {@link #getInputStream()} or {@link #getFile()}.
	 * </p>
	 * <p>
	 * 返回此资源的内容是否可读，如能否通过 {@link #getInputStream()} 或 {@link #getFile()} 读取资源。
	 * </p>
	 * 
	 * <p>
	 * Will be {@code true} for typical resource descriptors; note that actual content
	 * reading may still fail when attempted. However, a value of {@code false} is a
	 * definitive indication that the resource content cannot be read.
	 * </p>
	 * 
	 * @see #getInputStream()
	 */
	boolean isReadable();

	/**
	 * <p>
	 * Return whether this resource represents a handle with an open stream. If true, the
	 * InputStream cannot be read multiple times, and must be read and closed to avoid
	 * resource leaks.
	 * </p>
	 * <p>
	 * 返回这个资源是否有已打开流的句柄。如果为true，InputStream的不能被读取多次，并且必须读取和关闭，以避免资源泄漏。
	 * </p>
	 * 
	 * <p>
	 * Will be {@code false} for typical resource descriptors.
	 * </p>
	 */
	boolean isOpen();

	/**
	 * <p>
	 * {@link #getURL}、{@link #getURI}、{@link #getFile} 这3个方法可以实现为在 {@link java.net.URL
	 * URL}、{@link java.net.URI URI}、{@link java.io.File File} 之间转换 。
	 * </p>
	 * <hr>
	 * <p>
	 * Return a URL handle for this resource.
	 * </p>
	 * <p>
	 * 返回一个 {@link java.net.URL URL} 处理此资源。
	 * </p>
	 * 
	 * @throws IOException if the resource cannot be resolved as URL, i.e. if the resource
	 *         is not available as descriptor
	 */
	URL getURL() throws IOException;

	/**
	 * <p>
	 * {@link #getURL}、{@link #getURI}、{@link #getFile} 这3个方法可以实现为在 {@link java.net.URL
	 * URL}、{@link java.net.URI URI}、{@link java.io.File File} 之间转换 。
	 * </p>
	 * <hr>
	 * <p>
	 * Return a URI handle for this resource.
	 * </p>
	 * <p>
	 * 返回一个 {@link java.net.URI URI} 处理此资源。
	 * </p>
	 * 
	 * @throws IOException if the resource cannot be resolved as URI, i.e. if the resource
	 *         is not available as descriptor
	 */
	URI getURI() throws IOException;

	/**
	 * <p>
	 * {@link #getURL}、{@link #getURI}、{@link #getFile} 这3个方法可以实现为在 {@link java.net.URL
	 * URL}、{@link java.net.URI URI}、{@link java.io.File File} 之间转换 。
	 * </p>
	 * <hr>
	 * <p>
	 * Return a File handle for this resource.
	 * </p>
	 * <p>
	 * 返回一个 {@link java.io.File File} 处理此资源。
	 * </p>
	 * 
	 * @throws IOException if the resource cannot be resolved as absolute file path, i.e.
	 *         if the resource is not available in a file system
	 */
	File getFile() throws IOException;

	/**
	 * <p>
	 * Determine the content length for this resource.
	 * </p>
	 * <p>
	 * 确定这个资源的内容长度。
	 * </p>
	 * 
	 * @throws IOException if the resource cannot be resolved (in the file system or as
	 *         some other known physical resource type)
	 */
	long contentLength() throws IOException;

	/**
	 * <p>
	 * Determine the last-modified timestamp for this resource.
	 * </p>
	 * <p>
	 * 确定这个资源的最后一次修改的时间戳。
	 * </p>
	 * 
	 * @throws IOException if the resource cannot be resolved (in the file system or as
	 *         some other known physical resource type)
	 */
	long lastModified() throws IOException;

	/**
	 * <p>
	 * 或者以相对路径去理解相对资源。
	 * </p>
	 * <hr>
	 * <p>
	 * Create a resource relative to this resource.
	 * </p>
	 * <p>
	 * 创建基于该资源的相对资源。
	 * </p>
	 * 
	 * @param relativePath the relative path (relative to this resource)
	 * @return the resource handle for the relative resource
	 * @throws IOException if the relative resource cannot be determined
	 */
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * <p>
	 * Determine a filename for this resource, i.e. typically the last part of the path:
	 * for example, "myfile.txt".
	 * </p>
	 * <p>
	 * 确定这个资源的文件名，不是路径，而是其最后一部分：例如，“myfile.txt”。
	 * </p>
	 * 
	 * <p>
	 * Returns {@code null} if this type of resource does not have a filename.
	 */
	String getFilename();

	/**
	 * <p>
	 * Return a description for this resource, to be used for error output when working
	 * with the resource.
	 * </p>
	 * <p>
	 * 返回这个资源的描述信息，在资源工作时，将用于错误输出。
	 * </p>
	 * <p>
	 * Implementations are also encouraged to return this value from their
	 * {@code toString} method.
	 * 
	 * @see Object#toString()
	 */
	String getDescription();

}
