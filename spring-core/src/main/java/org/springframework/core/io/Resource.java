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
 * <h3>��Դ</h3>
 * <p>
 * �Ƕ� Spring ʹ�õ���Դ���������Ƚϵײ�ķ�װ��
 * </p>
 * <ul>
 * <li>���Ա�ʾ�����������͡�����λ�õ���Դ����������λ�õġ�����λ�õġ��ڴ��еĵȵȡ�����������ɸ��ӿ� {@link InputStreamSource} ����ģ���
 * {@link java.io.InputStream InputStream} ���͵���Դ��</li>
 * <li>����һЩ�����ķ�����������Դ�Ƿ񱻴򿪣������� {@link java.net.URL URL}��{@link java.net.URI URI}��
 * {@link java.io.File File} ֮��ת���ȡ�</li>
 * </ul>
 * <p>
 * ��һЩʵ�õ�ʵ���ࣺ{@link ByteArrayResource}�� Byte ������Դ����{@link ClassPathResource}����·����Դ����
 * {@link FileSystemResource}�������ļ���Դ����{@link InputStreamResource}�� InputStream ��Դ����
 * {@link UrlResource}�� URL ��Դ����
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
	 * ���������Դ�Ƿ�ʵ��������ڡ�
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
	 * ���ش���Դ�������Ƿ�ɶ������ܷ�ͨ�� {@link #getInputStream()} �� {@link #getFile()} ��ȡ��Դ��
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
	 * ���������Դ�Ƿ����Ѵ����ľ�������Ϊtrue��InputStream�Ĳ��ܱ���ȡ��Σ����ұ����ȡ�͹رգ��Ա�����Դй©��
	 * </p>
	 * 
	 * <p>
	 * Will be {@code false} for typical resource descriptors.
	 * </p>
	 */
	boolean isOpen();

	/**
	 * <p>
	 * {@link #getURL}��{@link #getURI}��{@link #getFile} ��3����������ʵ��Ϊ�� {@link java.net.URL
	 * URL}��{@link java.net.URI URI}��{@link java.io.File File} ֮��ת�� ��
	 * </p>
	 * <hr>
	 * <p>
	 * Return a URL handle for this resource.
	 * </p>
	 * <p>
	 * ����һ�� {@link java.net.URL URL} �������Դ��
	 * </p>
	 * 
	 * @throws IOException if the resource cannot be resolved as URL, i.e. if the resource
	 *         is not available as descriptor
	 */
	URL getURL() throws IOException;

	/**
	 * <p>
	 * {@link #getURL}��{@link #getURI}��{@link #getFile} ��3����������ʵ��Ϊ�� {@link java.net.URL
	 * URL}��{@link java.net.URI URI}��{@link java.io.File File} ֮��ת�� ��
	 * </p>
	 * <hr>
	 * <p>
	 * Return a URI handle for this resource.
	 * </p>
	 * <p>
	 * ����һ�� {@link java.net.URI URI} �������Դ��
	 * </p>
	 * 
	 * @throws IOException if the resource cannot be resolved as URI, i.e. if the resource
	 *         is not available as descriptor
	 */
	URI getURI() throws IOException;

	/**
	 * <p>
	 * {@link #getURL}��{@link #getURI}��{@link #getFile} ��3����������ʵ��Ϊ�� {@link java.net.URL
	 * URL}��{@link java.net.URI URI}��{@link java.io.File File} ֮��ת�� ��
	 * </p>
	 * <hr>
	 * <p>
	 * Return a File handle for this resource.
	 * </p>
	 * <p>
	 * ����һ�� {@link java.io.File File} �������Դ��
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
	 * ȷ�������Դ�����ݳ��ȡ�
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
	 * ȷ�������Դ�����һ���޸ĵ�ʱ�����
	 * </p>
	 * 
	 * @throws IOException if the resource cannot be resolved (in the file system or as
	 *         some other known physical resource type)
	 */
	long lastModified() throws IOException;

	/**
	 * <p>
	 * ���������·��ȥ��������Դ��
	 * </p>
	 * <hr>
	 * <p>
	 * Create a resource relative to this resource.
	 * </p>
	 * <p>
	 * �������ڸ���Դ�������Դ��
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
	 * ȷ�������Դ���ļ���������·�������������һ���֣����磬��myfile.txt����
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
	 * ���������Դ��������Ϣ������Դ����ʱ�������ڴ��������
	 * </p>
	 * <p>
	 * Implementations are also encouraged to return this value from their
	 * {@code toString} method.
	 * 
	 * @see Object#toString()
	 */
	String getDescription();

}
