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

import java.io.IOException;
import java.io.InputStream;

/**
 * <h3>资源的字节流输入源</h3>
 * <p>
 * 对资源的很基础的封装，表示各种能返回 {@link InputStream} 资源，比如有 {@link java.io.File File} 、
 * {@link java.lang.Byte Byte}、{@link java.lang.reflect.Array Array}。在 Spring 中对资源的描述，发更大挥作用是其子接口 {@link Resource} 。
 * </p>
 * <hr>
 * <p>
 * Simple interface for objects that are sources for an {@link InputStream}.
 * </p>
 * <p>
 * 简单的接口，封装了 {@link InputStream} 类型的资源。
 * </p>
 * 
 * <p>
 * This is the base interface for Spring's more extensive {@link Resource} interface.
 * </p>
 * <p>
 * 这只是基本的接口，其子接口 {@link Resource} 拥有对 Spring 的资源更广泛通用的封装。
 * </p>
 * 
 * <p>
 * For single-use streams, {@link InputStreamResource} can be used for any given
 * {@code InputStream}. Spring's {@link ByteArrayResource} or any file-based
 * {@code Resource} implementation can be used as a concrete instance, allowing one to
 * read the underlying content stream multiple times. This makes this interface useful as
 * an abstract content source for mail attachments, for example.
 * 
 * @author Juergen Hoeller
 * @since 20.01.2004
 * @see java.io.InputStream
 * @see Resource
 * @see InputStreamResource
 * @see ByteArrayResource
 */
public interface InputStreamSource {

	/**
	 * <p>
	 * Return an {@link InputStream}.
	 * </p>
	 * <p>
	 * 返回一个资源的输入流 {@link InputStream} 。
	 * </p>
	 * <p>
	 * It is expected that each call creates a <i>fresh</i> stream.
	 * <p>
	 * This requirement is particularly important when you consider an API such as
	 * JavaMail, which needs to be able to read the stream multiple times when creating
	 * mail attachments. For such a use case, it is <i>required</i> that each
	 * {@code getInputStream()} call returns a fresh stream.
	 * 
	 * @return the input stream for the underlying resource (must not be {@code null})
	 * @throws IOException if the stream could not be opened
	 * @see org.springframework.mail.javamail.MimeMessageHelper#addAttachment(String,
	 *      InputStreamSource)
	 */
	InputStream getInputStream() throws IOException;

}
