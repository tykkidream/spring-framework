/*
 * Copyright 2002-2013 the original author or authors.
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
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface.
 * Reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code &lt;beans&gt;} doesn't need to be the root
 * element of the XML document: This class will parse all bean definition elements
 * in the XML file, not regarding the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	public static final String NESTED_BEANS_ELEMENT = "beans";

	/**
	 * <h3>aliasԪ������</h3>
	 * <p>
	 * �ڶ�bean����ʱ������ʹ��id������ָ�������⣬Ϊ���ṩ������ƣ�����ʹ��alias��ǩ��ָ����
	 * �����е���Щ���ƶ�ָ��ͬһ������ĳЩ����±����ǳ����� ������Ϊ����Ӧ�õ�ÿһ�������
	 * �����׵ضԹ�������������á�
	 * </p>
	 * <p>
	 * Ȼ�����ڶ���beanʱ��ָ�����еı�����������ǡ���ġ���ʱ�������������ڵ�ǰλ��Ϊ��Щ�ڱ�
	 * �������bean�����������XML�����ļ��У����õ�����&lt;alias>Ԫ�������bean�����Ķ��塣�磺
	 * <pre>
	 * &lt;bean id="testBean" class="com.test"/>
	 * </pre>
	 * Ҫ�����JavaBean���ӱ������Է��㲻ͬ���������á����ǾͿ���ֱ��ʹ��bean��ǩ�е�name���ԣ�
	 * <pre>
	 * &lt;bean id="testBean" class="com.test" name="testBean,testBean2"/>
	 * </pre>
	 * ͬ����Spring��������һ�����������ķ�ʽ��
	 * <pre>
	 * &lt;bean id="testBean" class="com.test"/>
	 * &lt;alias name="testBean" alias="testBean,testBean2"/>
	 * </pre>
	 * ����һ����Ϊ��������ӣ����A��XML�����ļ��ж�����һ����ΪcomponetA��DataSource���͵�bean��
	 * �����Bȴ������XML�ļ�����componentBȫ�������ô�bean��������������MyApp��XML�����ļ��У�
	 * ϣ����myApp�����������ô�bean�������������3��XML���������յ�ApplicationContext���ڴ�����£�
	 * ��ͨ���������ļ����������aliasԪ����ʵ�֣�
	 * <pre>
	 * &lt;alias name="componentA" alias="componentB"/>
	 * &lt;alias name="componentA" alias="myApp"/>
	 * </pre>
	 * ����һ����ÿ�������������Ϳ�ͨ��Ψһ����������ͬһ������Դ���������š�
	 * </p>
	 * <p>���Ա����Ķ��������ַ�ʽ��һ����beanԪ�ص�name���ԣ�һ����aliasԪ�ء�</p>
	 */
	public static final String ALIAS_ELEMENT = "alias";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String ALIAS_ATTRIBUTE = "alias";

	/**
	 * <h3>importԪ������</h3>
	 * <p>����Ŀ����Ӵ�ʱ������̫��������ļ������Էֳɶ��ģ���������ļ���
	 * ��importԪ�ؿɽ�����ļ���ϵ��һ�𣬼��е�һ���������С�
	 * <pre>
	 * &lt;import resource="customerContext.xml" />
	 * </pre>
	 * </p>
	 */
	public static final String IMPORT_ELEMENT = "import";

	public static final String RESOURCE_ATTRIBUTE = "resource";

	/**
	 * <h3>profileԪ������</h3>
	 * <p>profile���÷���
	 * <p>��Spring��XML�����ļ���������������beans��
	 * <pre>
	 * &lt;beans profile="dev">......&lt;/beans>
	 * &lt;beans profile="pro">......&lt;/beans>
	 * </pre>
	 * ��ʱ����web.xml�п����������ã�
	 * <pre>
	 * &lt;context-param>
	 * &#9;&lt;param-name>Spring.profiles.active&lt;/param-name>
	 * &#9;&lt;param-value>dev&lt;/param-value>
	 * &lt;/context-param>
	 * </pre>
	 * ��ʾʹ��Spring��XML��������Ϊdev�����ã��������ԡ������Ϳ������ö���汾�������������������ġ����������ģ�
	 * �ܷ�����л���ͬ���á�
	 * <p>
	 */
	public static final String PROFILE_ATTRIBUTE = "profile";


	protected final Log logger = LogFactory.getLog(getClass());

	private Environment environment;

	private XmlReaderContext readerContext;

	private BeanDefinitionParserDelegate delegate;


	/**
	 * {@inheritDoc}
	 * <p>Default value is {@code null}; property is required for parsing any
	 * {@code <beans/>} element with a {@code profile} attribute present.
	 * @see #doRegisterBeanDefinitions
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * <p>����ע��Bean��
	 * <hr>
	 * 
	 * {@inheritDoc}
	 * <p>This implementation parses bean definitions according to the "spring-beans" XSD
	 * (or DTD, historically).
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at the {@code <beans/>} level; then parses the contained bean definitions.
	 */
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;
		logger.debug("Loading bean definitions");
		// ��ȡXML�ĸ���㡣
		Element root = doc.getDocumentElement();
		// ���������Ϊ��������ע�ᡣ�������������Ŀ�ʼ�����ˡ�
		doRegisterBeanDefinitions(root);
	}


	/**
	 * <p>�����ؽ���Bean��
	 * <hr>
	 * 
	 * Register each bean definition within the given root {@code <beans/>} element.
	 * @throws IllegalStateException if {@code <beans profile="..."} attribute is present
	 * and Environment property has not been set
	 * @see #setEnvironment
	 */
	protected void doRegisterBeanDefinitions(Element root) {
		// һ������profile���ԡ�
		String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
		// 1.1 ���beans�ڵ��Ƿ�����profile���ԡ�
		if (StringUtils.hasText(profileSpec)) {
			// ���������profile���ԡ�
			// 1.2 ��黷�������Ƿ���á�
			Assert.state(this.environment != null, "Environment must be set for evaluating profiles");
			// 1.3 ��web.xml�У�profile�ǲ����ǿ���ָ������ģ���������֡�
			String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
					profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
			// 1.4 ��鵱ǰ�����XML���Element��profile�Ƿ�ͬ����������ͬ��
			if (!this.environment.acceptsProfiles(specifiedProfiles)) {
				// �������web.xml��ָ�������ã����˳����������˷����ܡ�
				return;
			}
		}

		// Any nested <beans> elements will cause recursion in this method. In
		// order to propagate and preserve <beans> default-* attributes correctly,
		// keep track of the current (parent) delegate, which may be null. Create
		// the new (child) delegate with a reference to the parent for fallback purposes,
		// then ultimately reset this.delegate back to its original (parent) reference.
		// this behavior emulates a stack of delegates without actually necessitating one.
		// ר�Ŵ��������
		BeanDefinitionParserDelegate parent = this.delegate;
		this.delegate = createDelegate(this.readerContext, root, parent);

		// ����ǰ������������ʵ�֡�
		preProcessXml(root);
		parseBeanDefinitions(root, this.delegate);
		// ���������������� ʵ�֡�
		postProcessXml(root);

		this.delegate = parent;
	}

	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate) {

		BeanDefinitionParserDelegate delegate = createHelper(readerContext, root, parentDelegate);
		if (delegate == null) {
			delegate = new BeanDefinitionParserDelegate(readerContext, this.environment);
			delegate.initDefaults(root, parentDelegate);
		}
		return delegate;
	}

	@Deprecated
	protected BeanDefinitionParserDelegate createHelper(
			XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate) {

		return null;
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor} to pull the
	 * source metadata from the supplied {@link Element}.
	 */
	protected Object extractSource(Element ele) {
		return this.readerContext.extractSource(ele);
	}


	/**
	 * <p>
	 * <hr>
	 * 
	 * Parse the elements at the root level in the document:
	 * "import", "alias", "bean".
	 * @param root the DOM root element of the document
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		if (delegate.isDefaultNamespace(root)) {
			// ������ڵ���Ĭ�ϵ������ռ䡣
			// ��ȡ���ڵ���ӽڵ㡣
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				// �����õ�һ���ӽڵ㡣
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element ele = (Element) node;
					if (delegate.isDefaultNamespace(ele)) {
						// ������ڵ㡢�ӽڵ���Ĭ�ϵ������ռ䡣ʹ��Spring�Լ����˽⣨Ĭ�ϣ��ķ�ʽ������
						parseDefaultElement(ele, delegate);
					}
					else {
						// ����Spring����ʶ�ķ�Ĭ�ϵ������ռ䣬ʹ���Զ���ķ�ʽ�������Զ���������ռ䣩��
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		else {
			// ����Spring����ʶ�ķ�Ĭ�ϵ������ռ䣬ʹ���Զ���ķ�ʽ�������Զ���������ռ䣩��
			delegate.parseCustomElement(root);
		}
	}

	/**
	 * <p>����Spring��Ĭ�ϱ�ǩ���ֱ���import��alias��bean��beans��ǩ��
	 * 
	 * @param ele
	 * @param delegate
	 */
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		// ��import��ǩ����
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);
		}
		// ��alias��ǩ����
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);
		}
		// ��bean��ǩ����
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			processBeanDefinition(ele, delegate);
		}
		// ��beans��ǩ����
		else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// recurse
			// �ݹ鵽ǰ��Ľ���beans�ķ�����������һ�ν�����
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * <p>��importԪ�ؽ��н�����
	 * <hr>
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 */
	protected void importBeanDefinitionResource(Element ele) {
		// ��ȡresource����
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		// ���������resource���������κδ���
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// Resolve system properties: e.g. "${user.dir}"
		// ����ϵͳ���ԣ���ʽ�磺��${user.dir}����
		location = environment.resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<Resource>(4);

		// Discover whether the location is an absolute or relative URI
		// �ж�location�Ǿ���URI��ʱ���URI��
		boolean absoluteLocation = false;
		try {
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		}
		catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		// ����Ǿ���UIR��ֱ�Ӹ��ݵ�ַ���ض�Ӧ�������ļ���
		if (absoluteLocation) {
			try {
				// �ݹ����bean�Ľ������̣�������һ�ν�����
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		else {
			// No URL -> considering resource location as relative to the current file.
			// ��������URI�������Ե�ַ��������Ե�ַ��
			try {
				int importCount;
				// Resource���ڶ����ʵ���࣬��VfsResource��FileSystemResource�ȡ�
				// ��ÿ��resource��createRelative��ʽʵ�ֶ���һ��������������ʹ������ķ������Խ�����
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {
					// �ݹ����bean�Ľ������̣�������һ�ν�����
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				}
				else {
					// ����������ɹ�����ʹ��Ĭ�ϵĽ�����ResourcePatternResolver���н�����
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			}
			catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]",
						ele, ex);
			}
		}
		// �������м����������
		Resource[] actResArray = actualResources.toArray(new Resource[actualResources.size()]);
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * <p>��alias��ǩ���н�����
	 * <p>alias��ǩ�Ǳ���Ԫ�أ��Ƕ������������һ�ַ�ʽ��
	 * ���⻹��һ�ַ�ʽ����beanԪ�ص�name�����ж��壬
	 * �����ֵĽ������ڶ�beanԪ�صĽ�������ɵġ�
	 * <hr>
	 * 
	 * Process the given alias element, registering the alias with the registry.
	 */
	protected void processAliasRegistration(Element ele) {
		// ��ȡbeanName
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		// ��ȡalias
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				// ע��alias
				getReaderContext().getRegistry().registerAlias(name, alias);
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			// ����ע���֪ͨ����������Ӧ����
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * <p>��bean��ǩ���������
	 * <hr>
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		// ��һ����Ԫ�ؽ��н������õ�bdHolder�����������еĸ������ԣ���class��name��id��alias�ȡ�
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			// ��������bdHolder��Ϊ�յ�����£��ӽڵ����������Զ������ԣ�����Ҫ���Զ����ǩ���н�����
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// ������������ɺ���Ҫ�Խ������bdHolder����ע�ᡣ
				// Register the final decorated instance.
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// ���ģ������¼���֪ͨ��صļ����������bean�Ѿ�������ɡ�
			// Send registration event.
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * <p>����ǰ������������ʵ�֡�ģ�淽��ģʽ��
	 * <hr>
	 * 
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * <p>���������������� ʵ�֡�ģ�淽��ģʽ��
	 * <hr>
	 * 
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
