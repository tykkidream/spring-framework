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

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.EmptyReaderEventListener;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.NullSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.Assert;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.springframework.util.xml.XmlValidationModeDetector;

/**
 * <p>
 * 读取 XML 配置文件的是 Spring 的重要的的功能，因为 Spring 的大部分功能都被配置在 XML 配置文件。
 * </p>
 * 
 * 
 * <hr>
 * <p>
 * Bean definition reader for XML bean definitions. Delegates the actual XML document
 * reading to an implementation of the {@link BeanDefinitionDocumentReader} interface.
 * </p>
 * 
 * <p>
 * Typically applied to a
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory} or a
 * {@link org.springframework.context.support.GenericApplicationContext}.
 * </p>
 * 
 * <p>
 * This class loads a DOM document and applies the BeanDefinitionDocumentReader to it. The
 * document reader will register each bean definition with the given bean factory, talking
 * to the latter's implementation of the
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} interface.
 * 
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @since 26.11.2003
 * @see #setDocumentReaderClass
 * @see BeanDefinitionDocumentReader
 * @see DefaultBeanDefinitionDocumentReader
 * @see BeanDefinitionRegistry
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * Indicates that the validation should be disabled.
	 */
	public static final int VALIDATION_NONE = XmlValidationModeDetector.VALIDATION_NONE;

	/**
	 * <p>
	 * 等于 {@link XmlValidationModeDetector#VALIDATION_AUTO} 。值为1。
	 * </p>
	 * 
	 * <hr>
	 * <p>
	 * Indicates that the validation mode should be detected automatically.
	 * </p>
	 * <p>
	 * 表示验证模式应自动检测。
	 * </p>
	 */
	public static final int VALIDATION_AUTO = XmlValidationModeDetector.VALIDATION_AUTO;

	/**
	 * Indicates that DTD validation should be used.
	 */
	public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;

	/**
	 * Indicates that XSD validation should be used.
	 */
	public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;

	/** Constants instance for this class */
	private static final Constants constants = new Constants(
			XmlBeanDefinitionReader.class);

	private int validationMode = VALIDATION_AUTO;

	private boolean namespaceAware = false;

	private Class<?> documentReaderClass = DefaultBeanDefinitionDocumentReader.class;

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	private ReaderEventListener eventListener = new EmptyReaderEventListener();

	private SourceExtractor sourceExtractor = new NullSourceExtractor();

	private NamespaceHandlerResolver namespaceHandlerResolver;

	private DocumentLoader documentLoader = new DefaultDocumentLoader();

	private EntityResolver entityResolver;

	private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

	private final XmlValidationModeDetector validationModeDetector = new XmlValidationModeDetector();

	private final ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded = new NamedThreadLocal<Set<EncodedResource>>(
			"XML bean definition resources currently being loaded");

	/**
	 * Create new XmlBeanDefinitionReader for the given bean factory.
	 * 
	 * @param registry the BeanFactory to load bean definitions into, in the form of a
	 *        BeanDefinitionRegistry
	 */
	public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}

	/**
	 * Set whether to use XML validation. Default is {@code true}.
	 * <p>
	 * This method switches namespace awareness on if validation is turned off, in order
	 * to still process schema namespaces properly in such a scenario.
	 * 
	 * @see #setValidationMode
	 * @see #setNamespaceAware
	 */
	public void setValidating(boolean validating) {
		this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
		this.namespaceAware = !validating;
	}

	/**
	 * Set the validation mode to use by name. Defaults to {@link #VALIDATION_AUTO}.
	 * 
	 * @see #setValidationMode
	 */
	public void setValidationModeName(String validationModeName) {
		setValidationMode(constants.asNumber(validationModeName).intValue());
	}

	/**
	 * Set the validation mode to use. Defaults to {@link #VALIDATION_AUTO}.
	 * <p>
	 * Note that this only activates or deactivates validation itself. If you are
	 * switching validation off for schema files, you might need to activate schema
	 * namespace support explicitly: see {@link #setNamespaceAware}.
	 */
	public void setValidationMode(int validationMode) {
		this.validationMode = validationMode;
	}

	/**
	 * <p>
	 * 默认的验证模式是 {@link #VALIDATION_AUTO} 。
	 * </p>
	 * <hr>
	 * <p>
	 * Return the validation mode to use.
	 * <p>
	 * <p>
	 * 返回验证模式来使用。
	 * </p>
	 */
	public int getValidationMode() {
		return this.validationMode;
	}

	/**
	 * Set whether or not the XML parser should be XML namespace aware. Default is
	 * "false".
	 * <p>
	 * This is typically not needed when schema validation is active. However, without
	 * validation, this has to be switched to "true" in order to properly process schema
	 * namespaces.
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	/**
	 * Return whether or not the XML parser should be XML namespace aware.
	 */
	public boolean isNamespaceAware() {
		return this.namespaceAware;
	}

	/**
	 * Specify which {@link org.springframework.beans.factory.parsing.ProblemReporter} to
	 * use.
	 * <p>
	 * The default implementation is
	 * {@link org.springframework.beans.factory.parsing.FailFastProblemReporter} which
	 * exhibits fail fast behaviour. External tools can provide an alternative
	 * implementation that collates errors and warnings for display in the tool UI.
	 */
	public void setProblemReporter(ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter
				: new FailFastProblemReporter());
	}

	/**
	 * Specify which {@link ReaderEventListener} to use.
	 * <p>
	 * The default implementation is EmptyReaderEventListener which discards every event
	 * notification. External tools can provide an alternative implementation to monitor
	 * the components being registered in the BeanFactory.
	 */
	public void setEventListener(ReaderEventListener eventListener) {
		this.eventListener = (eventListener != null ? eventListener
				: new EmptyReaderEventListener());
	}

	/**
	 * Specify the {@link SourceExtractor} to use.
	 * <p>
	 * The default implementation is {@link NullSourceExtractor} which simply returns
	 * {@code null} as the source object. This means that - during normal runtime
	 * execution - no additional source metadata is attached to the bean configuration
	 * metadata.
	 */
	public void setSourceExtractor(SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor
				: new NullSourceExtractor());
	}

	/**
	 * Specify the {@link NamespaceHandlerResolver} to use.
	 * <p>
	 * If none is specified, a default instance will be created through
	 * {@link #createDefaultNamespaceHandlerResolver()}.
	 */
	public void setNamespaceHandlerResolver(
			NamespaceHandlerResolver namespaceHandlerResolver) {
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}

	/**
	 * Specify the {@link DocumentLoader} to use.
	 * <p>
	 * The default implementation is {@link DefaultDocumentLoader} which loads
	 * {@link Document} instances using JAXP.
	 */
	public void setDocumentLoader(DocumentLoader documentLoader) {
		this.documentLoader = (documentLoader != null ? documentLoader
				: new DefaultDocumentLoader());
	}

	/**
	 * Set a SAX entity resolver to be used for parsing.
	 * <p>
	 * By default, {@link ResourceEntityResolver} will be used. Can be overridden for
	 * custom entity resolution, for example relative to some specific base path.
	 */
	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	/**
	 * <p>
	 * Return the EntityResolver to use, building a default resolver if none specified.
	 * </p>
	 * <p>
	 * 返回可用的 {@link EntityResolver}，如果未指定则构建一个默认的解析器。
	 * </p>
	 */
	protected EntityResolver getEntityResolver() {
		if (this.entityResolver == null) {
			// Determine default EntityResolver to use.
			// 如果当前 entityResolver 实体解析器不存在，那就定义一个默认的使用。
			// 获取资源加载器
			ResourceLoader resourceLoader = getResourceLoader();
			if (resourceLoader != null) {
				// 根据
				this.entityResolver = new ResourceEntityResolver(resourceLoader);
			}
			else {
				this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
			}
		}
		return this.entityResolver;
	}

	/**
	 * Set an implementation of the {@code org.xml.sax.ErrorHandler} interface for custom
	 * handling of XML parsing errors and warnings.
	 * <p>
	 * If not set, a default SimpleSaxErrorHandler is used that simply logs warnings using
	 * the logger instance of the view class, and rethrows errors to discontinue the XML
	 * transformation.
	 * 
	 * @see SimpleSaxErrorHandler
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Specify the {@link BeanDefinitionDocumentReader} implementation to use, responsible
	 * for the actual reading of the XML bean definition document.
	 * <p>
	 * The default is {@link DefaultBeanDefinitionDocumentReader}.
	 * 
	 * @param documentReaderClass the desired BeanDefinitionDocumentReader implementation
	 *        class
	 */
	public void setDocumentReaderClass(Class<?> documentReaderClass) {
		if (documentReaderClass == null
				|| !BeanDefinitionDocumentReader.class.isAssignableFrom(documentReaderClass)) {
			throw new IllegalArgumentException(
					"documentReaderClass must be an implementation of the BeanDefinitionDocumentReader interface");
		}
		this.documentReaderClass = documentReaderClass;
	}

	/**
	 * <h3>加载定义 Bean 的资源文件</h3>
	 * <p>
	 * 使用 {@link EncodedResource} 包装 {@link Resource} 参数，然后由
	 * {@link #loadBeanDefinitions(EncodedResource)} 处理。
	 * </p>
	 * <hr>
	 * <p>
	 * Load bean definitions from the specified XML file.
	 * </p>
	 * <p>
	 * 从指定的XML文件加载 Bean 定义。
	 * </p>
	 * 
	 * @param resource <span>the resource descriptor for the XML file. </span><span>描述了
	 *        Bean 定义的 XML 配置文件。</span>
	 * @return <span>the number of bean definitions found.
	 *         </span><span>发现的bean定义的数。</span>
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		// 为了处理编码，使用 EncodedResource 对资源再进行包装。
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * <h3>加载定义 Bean 的编码处理的资源文件</h3>
	 * <p>
	 * </p>
	 * <hr>
	 * <p>
	 * Load bean definitions from the specified XML file.
	 * </p>
	 * <p>
	 * 从指定的XML文件加载 bean 定义。
	 * </p>
	 * 
	 * @param encodedResource the resource descriptor for the XML file, allowing to
	 *        specify an encoding to use for parsing the file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource)
			throws BeanDefinitionStoreException {
		// 验证编码资源文件不为 Null 。
		Assert.notNull(encodedResource, "EncodedResource must not be null");
		if (logger.isInfoEnabled()) {
			// 如果日志可用，输出信息。
			logger.info("Loading XML bean definitions from "
					+ encodedResource.getResource());
		}

		// 将编码资源文件添加到当前管理的资源文件集合中。
		Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
		if (currentResources == null) {
			currentResources = new HashSet<EncodedResource>(4);
			this.resourcesCurrentlyBeingLoaded.set(currentResources);
		}
		if (!currentResources.add(encodedResource)) {
			throw new BeanDefinitionStoreException("Detected cyclic loading of "
					+ encodedResource + " - check your import definitions!");
		}

		// 这里是本方法的主要业务，共3步：
		try {
			// 1、获取资源的 InputStream 输入流。
			InputStream inputStream = encodedResource.getResource().getInputStream();
			try {
				// 2、封装资源的 InputStream 输入流成为 InputSource 。
				// InputSource 是 XML 实体的单一输入源。是 Java 的 SAX 解析的 API 。
				InputSource inputSource = new InputSource(inputStream);
				if (encodedResource.getEncoding() != null) {
					// 设置字符编码。
					inputSource.setEncoding(encodedResource.getEncoding());
				}
				// 3、真正的从 XML 文件加载 bean 定义。
				return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"IOException parsing XML document from "
							+ encodedResource.getResource(), ex);
		}
		finally {
			currentResources.remove(encodedResource);
			if (currentResources.isEmpty()) {
				this.resourcesCurrentlyBeingLoaded.remove();
			}
		}
	}

	/**
	 * Load bean definitions from the specified XML file.
	 * 
	 * @param inputSource the SAX InputSource to read from
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(InputSource inputSource)
			throws BeanDefinitionStoreException {
		return loadBeanDefinitions(inputSource, "resource loaded through SAX InputSource");
	}

	/**
	 * Load bean definitions from the specified XML file.
	 * 
	 * @param inputSource the SAX InputSource to read from
	 * @param resourceDescription a description of the resource (can be {@code null} or
	 *        empty)
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(InputSource inputSource, String resourceDescription)
			throws BeanDefinitionStoreException {

		return doLoadBeanDefinitions(inputSource, new DescriptiveResource(
				resourceDescription));
	}

	/**
	 * <h3>加载定义 Bean 的 SAX 的 XML 输入源</h3>
	 * 
	 * Actually load bean definitions from the specified XML file.
	 * 
	 * @param inputSource the SAX InputSource to read from
	 * @param resource the resource descriptor for the XML file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
			throws BeanDefinitionStoreException {
		try {
			// 得到 XML的验证模式。
			int validationMode = getValidationModeForResource(resource);
			// 加载 XML ，获取它的 Document 。
			Document doc = this.documentLoader.loadDocument(inputSource,
					getEntityResolver(), this.errorHandler, validationMode,
					isNamespaceAware());
			// 注册 Bean 定义。
			return registerBeanDefinitions(doc, resource);
		}
		catch (BeanDefinitionStoreException ex) {
			throw ex;
		}
		catch (SAXParseException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(), "Line "
					+ ex.getLineNumber() + " in XML document from " + resource
					+ " is invalid", ex);
		}
		catch (SAXException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"XML document from " + resource + " is invalid", ex);
		}
		catch (ParserConfigurationException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Parser configuration exception parsing XML from " + resource, ex);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"IOException parsing XML document from " + resource, ex);
		}
		catch (Throwable ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Unexpected exception parsing XML document from " + resource, ex);
		}
	}

	/**
	 * <h3>得到 XML的验证模式</h3>
	 * <p>
	 * 默认是自动检测验证模式，首先考虑使用手工指定的验证模式，未指定时读取 XML 文件，检测其中配置的验证模式， 如果也没有检测出来，那就使用 XSD 的验证模式。
	 * <p>
	 * <h4>参考</h4>
	 * <ul>
	 * <li>获取手工指定的验证模式： {@link #getValidationMode()} 。</li>
	 * <li>默认的自动检测验证模式： {@link #VALIDATION_AUTO} 。</li>
	 * <li>检测 XML 中的验证模式： {@link #detectValidationMode(Resource)} 。</li>
	 * <li>最后的 XSD 验证模式： {@link #VALIDATION_XSD} 。</li>
	 * </ul>
	 * <hr>
	 * <p>
	 * Gets the validation mode for the specified {@link Resource}. If no explicit
	 * validation mode has been configured then the validation mode is
	 * {@link #detectValidationMode detected}.
	 * </p>
	 * <p>
	 * Override this method if you would like full control over the validation mode, even
	 * when something other than {@link #VALIDATION_AUTO} was set.
	 * </p>
	 */
	protected int getValidationModeForResource(Resource resource) {
		// 使用验证模式。默认是自动检测验证模式。
		int validationModeToUse = getValidationMode();
		// 如果手工指定了验证模式，不使用自动检测。
		if (validationModeToUse != VALIDATION_AUTO) {
			// 那么使用手工指定的验证模式。
			return validationModeToUse;
		}

		// 开始版检测验证模式，读取 XML 中的验证模式。
		int detectedMode = detectValidationMode(resource);
		// 如果 XML 中有指定并检测出可用的验证模式，不是自动检测。
		if (detectedMode != VALIDATION_AUTO) {
			return detectedMode;
		}

		// XML 中有也没有指定验证模式，那就使用 XSD 的方式。
		// Hmm, we didn't get a clear indication... Let's assume XSD,
		// since apparently no DTD declaration has been found up until
		// detection stopped (before finding the document's root tag).
		return VALIDATION_XSD;
	}

	/**
	 * <h3>读取（ XML ）资源，检测其验证模式</h3>
	 * <p>
	 * 使用时，资源不能是被打开的，能获取有效的输入流，最后由给内部 XmlValidationModeDetector 实例验证，无法验证出时会返回
	 * XmlValidationModeDetector.VALIDATION_AUTO 。 XmlValidationModeDetector 类属于 SpringFramework 的 Core 模块，专用于处理
	 * XML 文件，它的实例在本类是私有的，定义时同时就声明实例化了，仅在这里发挥了作用。
	 * </p>
	 * <h4>参考</h4>
	 * <ul>
	 * <li>检查资源是否已经被打开： {@link Resource#isOpen()} 。</li>
	 * <li>获取资源的有效的输入流： {@link Resource#getInputStream()} 。</li>
	 * <li>验证 XML 文件的工具类： {@link XmlValidationModeDetector} 。</li>
	 * <li>验证 XML 文件的方法： {@link XmlValidationModeDetector#detectValidationMode(InputStream)} 。</li>
	 * <li>无法得出验证的验证模式： {@link XmlValidationModeDetector#VALIDATION_AUTO} 。</li>
	 * </ul>
	 * 
	 * <hr>
	 * <p>
	 * Detects which kind of validation to perform on the XML file identified by the
	 * supplied {@link Resource}. If the file has a {@code DOCTYPE} definition then DTD
	 * validation is used otherwise XSD validation is assumed.
	 * </p>
	 * <p>
	 * Override this method if you would like to customize resolution of the
	 * </p>
	 * {@link #VALIDATION_AUTO} mode.
	 */
	protected int detectValidationMode(Resource resource) {
		// 如果资源已经被其它地方打开使用。
		if (resource.isOpen()) {
			throw new BeanDefinitionStoreException(
					"Passed-in Resource ["
							+ resource
							+ "] contains an open stream: "
							+ "cannot determine validation mode automatically. Either pass in a Resource "
							+ "that is able to create fresh streams, or explicitly specify the validationMode "
							+ "on your XmlBeanDefinitionReader instance.");
		}

		// 资源的输入源。
		InputStream inputStream;
		try {
			inputStream = resource.getInputStream();
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"Unable to determine validation mode for ["
							+ resource
							+ "]: cannot open InputStream. "
							+ "Did you attempt to load directly from a SAX InputSource without specifying the "
							+ "validationMode on your XmlBeanDefinitionReader instance?",
					ex);
		}

		try {
			// 委托给 XmlValidationModeDetector 进行验证。 
			return this.validationModeDetector.detectValidationMode(inputStream);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"Unable to determine validation mode for [" + resource
							+ "]: an error occurred whilst reading from the InputStream.",
					ex);
		}
	}

	/**
	 * Register the bean definitions contained in the given DOM document. Called by
	 * {@code loadBeanDefinitions}.
	 * <p>
	 * Creates a new instance of the parser class and invokes
	 * {@code registerBeanDefinitions} on it.
	 * 
	 * @param doc the DOM document
	 * @param resource the resource descriptor (for context information)
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of parsing errors
	 * @see #loadBeanDefinitions
	 * @see #setDocumentReaderClass
	 * @see BeanDefinitionDocumentReader#registerBeanDefinitions
	 */
	public int registerBeanDefinitions(Document doc, Resource resource)
			throws BeanDefinitionStoreException {
		BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
		documentReader.setEnvironment(this.getEnvironment());
		int countBefore = getRegistry().getBeanDefinitionCount();
		documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
		return getRegistry().getBeanDefinitionCount() - countBefore;
	}

	/**
	 * Create the {@link BeanDefinitionDocumentReader} to use for actually reading bean
	 * definitions from an XML document.
	 * <p>
	 * The default implementation instantiates the specified "documentReaderClass".
	 * 
	 * @see #setDocumentReaderClass
	 */
	protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
		return BeanDefinitionDocumentReader.class.cast(BeanUtils.instantiateClass(this.documentReaderClass));
	}

	/**
	 * Create the {@link XmlReaderContext} to pass over to the document reader.
	 */
	protected XmlReaderContext createReaderContext(Resource resource) {
		if (this.namespaceHandlerResolver == null) {
			this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
		}
		return new XmlReaderContext(resource, this.problemReporter, this.eventListener,
				this.sourceExtractor, this, this.namespaceHandlerResolver);
	}

	/**
	 * Create the default implementation of {@link NamespaceHandlerResolver} used if none
	 * is specified. Default implementation returns an instance of
	 * {@link DefaultNamespaceHandlerResolver}.
	 */
	protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
		return new DefaultNamespaceHandlerResolver(getResourceLoader().getClassLoader());
	}

}
