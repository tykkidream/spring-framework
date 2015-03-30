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

package org.springframework.beans.factory.support;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 *<p>对接口SingletonBeanRegistry各函数的实现。
 *<p>共享bean实例的通用注册表，实现了SingletonBeanRegistry。允许注册表中注册的单例
 *应该被所有调用者共享，通过bean名称获得。还支持登记的DisposableBean实例，（这可
 *能会或不能正确的注册单例），关闭注册表时destroyed。可以注册bean之间的依赖关系，
 *执行适当的关闭顺序。
 * <hr>
 * 
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/**
	 * <p>内部标记为一个空的单例对象： 并发 Maps( 不支持空值 )作为标志值。
	 * <hr>
	 * Internal marker for a null singleton object:
	 * used as marker value for concurrent Maps (which don't support null values).
	 */
	protected static final Object NULL_OBJECT = new Object();


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * <p>存放单例Bean的缓存。是ConcurrentHashMap<String, Boolean>类型。
	 * <hr>
	 * 
	 * Cache of singleton objects: bean name --> bean instance
	 **/
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(64);

	/**
	 * <p>存放制造单例Bean的工厂对象的缓存。是HashMap<String, ObjectFactory<?>>类型。
	 * <hr>
	 * 
	 * Cache of singleton factories: bean name --> ObjectFactory
	 **/
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>(16);

	/**
	 * <p>存放单例Bean工厂 制造出来的单例Bean的缓存。是HashMap<String, Object>类型。
	 * <hr>
	 * 
	 * Cache of early singleton objects: bean name --> bean instance
	 **/
	private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);

	/**
	 * <p>单例注册表。是LinkedHashSet<String>类型
	 * <hr>
	 * 
	 * Set of registered singletons, containing the bean names in registration order
	 **/
	private final Set<String> registeredSingletons = new LinkedHashSet<String>(64);

	/**
	 * <p>当前正在创建中的单例Bean的名称的集合。是ConcurrentHashMap<String, Boolean>类型。
	 * <hr>
	 * 
	 * Names of beans that are currently in creation (using a ConcurrentHashMap as a Set)
	 **/
	private final Map<String, Boolean> singletonsCurrentlyInCreation = new ConcurrentHashMap<String, Boolean>(16);

	/**
	 * <p>创建失败的Bean的名字集合。是ConcurrentHashMap<String, Boolean>类型。
	 * <hr>
	 * 
	 * Names of beans currently excluded from in creation checks (using a ConcurrentHashMap as a Set).
	 **/
	private final Map<String, Boolean> inCreationCheckExclusions = new ConcurrentHashMap<String, Boolean>(16);

	/**
	 * <p>存放异常出现的相关的原因的集合。
	 * <hr>
	 * 
	 * List of suppressed Exceptions, available for associating related causes
	 **/
	private Set<Exception> suppressedExceptions;

	/**
	 * <p>标志，指示我们目前是否在销毁单例中。
	 * <hr>
	 * 
	 * Flag that indicates whether we're currently within destroySingletons
	 **/
	private boolean singletonsCurrentlyInDestruction = false;

	/**
	 * <p>存放一次性bean的缓存。
	 * <hr>
	 * 
	 * Disposable bean instances: bean name --> disposable instance
	 **/
	private final Map<String, Object> disposableBeans = new LinkedHashMap<String, Object>();

	/**
	 * <p>外部Bean与被包含在外部Bean的所有内部Bean集合包含关系的缓存。
	 * <hr>
	 * 
	 * Map between containing bean names: bean name --> Set of bean names that the bean contains
	 **/
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<String, Set<String>>(16);

	/**
	 * <p>指定bean与依赖指定bean的所有bean的依赖关系的缓存。
	 * <hr>
	 * 
	 * Map between dependent bean names: bean name --> Set of dependent bean names
	 **/
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

	/**
	 * <p>指定Bean与创建这个Bean所需要依赖的所有Bean的依赖关系的缓存。
	 * <hr>
	 * 
	 * Map between depending bean names: bean name --> Set of bean names for the bean's dependencies
	 **/
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<String, Set<String>>(64);


	/*
	 * SingletonBeanRegistry接口的registerSingleton方法的实现。
	 */
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "'beanName' must not be null");
		synchronized (this.singletonObjects) {
			// 从单例缓存中找到单例Bean。
			Object oldObject = this.singletonObjects.get(beanName);
			if (oldObject != null) {
				// 如果该名称已经注册被占用，则抛出异常。
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			// 注册Bean。
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * <p>注册Bean，真正的注册操作。
	 * <hr>
	 * 
	 * Add the given singleton object to the singleton cache of this factory.
	 * <p>To be called for eager registration of singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			// 添加单例Bean。
			// 因为singletonObjects类型是ConcurrentHashMap，并发Map不支持空值作为标志值，所以用NULL_OBJECT来代替。
			this.singletonObjects.put(beanName, (singletonObject != null ? singletonObject : NULL_OBJECT));
			// beanName已被注册存放在singletonObjects缓存，那么singletonFactories不应该再持有名称为beanName的工厂。
			this.singletonFactories.remove(beanName);
			// beanName已被注册存放在singletonObjects缓存，那么earlySingletonObjects不应该再持有名称为beanName的bean。
			this.earlySingletonObjects.remove(beanName);
			// beanName放进单例注册表中。
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * <p>添加名称为beanName的单例Bean的工厂对象。
	 * <hr>
	 * 
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			if (!this.singletonObjects.containsKey(beanName)) {
				// 如果在singletonObjects中，beanName没有被占用，则可以进行注册操作。
				this.singletonFactories.put(beanName, singletonFactory);
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}

	/*
	 * SingletonBeanRegistry接口的getSingleton方法的实现
	 */
	public Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * <ul>
	 * <li>如果singletonObject中存在已注册的单例Bean，则返回它。</li>
	 * <li>如果singletonObject中不存在单例Bean，却正在被创建中，则返回null。</li>
	 * <li>如果singletonObject中不存在单例Bean，也不正在创建中，则从工厂创建的Bean的缓存earlySingletonObjects中找到并返回。</li>
	 * <li>如果工厂创建的Bean的缓存earlySingletonObjects中也不存在，当allowEarlyReference为true时则找到工厂创建一份并返回。</li>
	 * <li>如果工厂也不存在，返回null。</li>
	 * </ul>
	 * <hr>
	 * 
	 * Return the (raw) singleton object registered under the given name.
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not
	 * @return the registered singleton object, or {@code null} if none found
	 */
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// 从缓存中获取单例Bean。
		Object singletonObject = this.singletonObjects.get(beanName);
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			// 如果beanName的单例Bean不存在，同时也不是正在被创建中。
			synchronized (this.singletonObjects) {
				// 从工厂创建的Bean的缓存中找到Bean。
				singletonObject = this.earlySingletonObjects.get(beanName);
				if (singletonObject == null && allowEarlyReference) {
					// 工厂创建的Bean的缓存也没有Bean，allowEarlyReference表示允许使用工厂创建Bean。
					// 找到Bean的工厂。
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
						// 如果有这个Bean的工厂。
						// 工厂创建一个Bean。
						singletonObject = singletonFactory.getObject();
						// 向工厂创建的Bean的缓存中存放。
						this.earlySingletonObjects.put(beanName, singletonObject);
						// FIXME 这里表示指定的beanName已被占用，所以要在singletonFactories移除该名称。
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		
		// 如果Bean是空单例NULL_OBJECT，则表示为null。
		return (singletonObject != NULL_OBJECT ? singletonObject : null);
	}

	/**
	 * <p>
	 * <hr>
	 * 
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 */
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "'beanName' must not be null");
		synchronized (this.singletonObjects) {
			// 从缓存中获取单例Bean。
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				// 如果singetonObjects缓存不存在名称为beanName的单例Bean。
				// 如果目前在销毁单例Bean。
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while the singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				// 单例对象创建前的回调，默认实现注册正在创建的单例。
				beforeSingletonCreation(beanName);
				// 判断存储异常相关原因的集合是否已存在
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				// 若没有存储异常相关原因的集合，则创建异常集合的实例。
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<Exception>();
				}
				try {
					// 由参数给定了Bean工厂，则使用它创建单例Bean，getObject方法的具体实现由ObjectFactory的子类决定。
					singletonObject = singletonFactory.getObject();
				}
				catch (BeanCreationException ex) {
					// 如果异常被捕获，在这里将出现异常的原因抛出。
					if (recordSuppressedExceptions) {
						// recordSuppressedExceptions为true时，suppressedExceptions也就在之前被创建了。
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					// 结束前，将异常集合销毁掉。
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					// 单例创建之后的回调，默认的实现标志单例不要在创建了。
					afterSingletonCreation(beanName);
				}
				// 注册创建后的单例。
				addSingleton(beanName, singletonObject);
			}
			return (singletonObject != NULL_OBJECT ? singletonObject : null);
		}
	}

	/**
	 * <p>注册发生在单例Bean创建期间发生的异常。
	 * <hr>
	 * 
	 * Register an Exception that happened to get suppressed during the creation of a
	 * singleton bean instance, e.g. a temporary circular reference resolution problem.
	 * @param ex the Exception to register
	 */
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * <p>移除名称为beanName的单例，主要在四个集合中移除：{@link #singletonObjects}、{@link #singletonFactories}、
	 * {@link #earlySingletonObjects} 、{@link #registeredSingletons}。
	 * <hr>
	 * 
	 * Remove the bean with the given name from the singleton cache of this factory,
	 * to be able to clean up eager registration of a singleton if creation failed.
	 * @param beanName the name of the bean
	 * @see #getSingletonMutex()
	 */
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.remove(beanName);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.remove(beanName);
		}
	}

	/*
	 * singletonBeanRegistry接口的containsSingleton方法实现
	 * @see org.springframework.beans.factory.config.SingletonBeanRegistry#containsSingleton(java.lang.String)
	 */
	public boolean containsSingleton(String beanName) {
		return (this.singletonObjects.containsKey(beanName));
	}

	/*
	 * singletonBeanRegistry接口的getSingletonNames方法实现
	 * @see org.springframework.beans.factory.config.SingletonBeanRegistry#getSingletonNames()
	 */
	public String[] getSingletonNames() {
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	/*
	 * singletonBeanRegistry接口的getSingletonCount方法实现
	 * @see org.springframework.beans.factory.config.SingletonBeanRegistry#getSingletonCount()
	 */
	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			return this.registeredSingletons.size();
		}
	}


	/**
	 * 设置某个Bean创建是否失败。当 inCreation 为 false 时表示创建出现异常，
	 * 并将其添加到 {@link #inCreationCheckExclusions} 集合中，否则表示正常，
	 * 并从集合中移除。
	 * @param beanName
	 * @param inCreation
	 */
	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.put(beanName, Boolean.TRUE);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	/**
	 * <p>检测某个beanName的单例是否正在被创建中，并且没有异常。
	 * <hr>
	 * 
	 * @param beanName
	 * @return
	 */
	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.containsKey(beanName) && isActuallyInCreation(beanName));
	}

	/**
	 * <p>检测某个beanName的单例是否正在被创建中。
	 * 实际上就是{@link #isSingletonCurrentlyInCreation(String)};
	 * <hr>
	 * 
	 * @param beanName
	 * @return
	 */
	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * <p>检测某个beanName的单例是否正在被创建中。
	 * <hr>
	 * 
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 */
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.containsKey(beanName);
	}

	/**
	 * <p>单例对象创建前的回调，检测Bean是否处于创建中，或者之前创建失败发生了异常。
	 * <hr>
	 * 
	 * Callback before singleton creation.
	 * <p>Default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.containsKey(beanName) &&
				this.singletonsCurrentlyInCreation.put(beanName, Boolean.TRUE) != null) {
			// 如果inCreationCheckExclusions中不存在beanName，表示创建Bean有异常；
			// 如果singletonsCurrentlyInCreation中存在beanName，表示Bean处于创建中。
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * <p>单例创建之后的回调，检测Bean是否创建成功，或者之前创建之中发生了异常。
	 * <hr>
	 * 
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.containsKey(beanName) &&
				!this.singletonsCurrentlyInCreation.remove(beanName)) {
			// 如果inCreationCheckExclusions中不存在beanName，表示创建Bean有异常；
			// 如果singletonsCurrentlyInCreation中存在beanName，并且value为false，表示Bean创建失败。
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * <p>一次性Bean注册，存放在disponsableBeans集合中 
	 * <hr>
	 * 
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * <p>注册两个Bean之间的控制关系，例如内部Bean和包含其的外部Bean之间
	 * <hr>
	 * 
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			// 从containedBeanMap缓存中查找外部Bean名为containingBeanName的内部Bean集合
			Set<String> containedBeans = this.containedBeanMap.get(containingBeanName);
			if (containedBeans == null) {
				// 如果没有，刚新建一个存放内部bean的集合，并且存放在containedBeanMap缓存中
				containedBeans = new LinkedHashSet<String>(8);
				this.containedBeanMap.put(containingBeanName, containedBeans);
			}
			// 将名为containedBeanName的内部bean存放到内部bean集合
			containedBeans.add(containedBeanName);
		}
		// 紧接着调用注册内部Bean和外部Bean的依赖关系的方法
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * <p>注册给定bean的一个依赖bean，给定的bean销毁之前被销毁。
	 * <hr>
	 * 
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		// 调用父类SimpleAliasRegistry的canonicalName方法，将参数beanName当做别名寻找到注册名，并依此递归
		String canonicalName = canonicalName(beanName);
		synchronized (this.dependentBeanMap) {
			// 从dependentBeanMap缓存中找到依赖名为canonicalName这个Bean的依赖bean集合
			Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
			if (dependentBeans == null) {
				// 如果为空，则新建一个依赖Bean集合，并且存放到dependentBeanMap缓存中
				dependentBeans = new LinkedHashSet<String>(8);
				this.dependentBeanMap.put(canonicalName, dependentBeans);
			}
			// 依赖Bean集合添加dependentBeanName
			dependentBeans.add(dependentBeanName);
		}
		synchronized (this.dependenciesForBeanMap) {
			// 从dependenciesForBeanMap缓存中找到dependentBeanName要依赖的所有Bean集合
			Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(dependentBeanName);
			if (dependenciesForBean == null) {
				dependenciesForBean = new LinkedHashSet<String>(8);
				this.dependenciesForBeanMap.put(dependentBeanName, dependenciesForBean);
			}
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * <p>确定是否还存在名为beanName的被依赖关系
	 * <hr>
	 * 
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * <p>返回依赖于指定的bean的所有bean的名称，如果有的话。 
	 * <hr>
	 * 
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		return StringUtils.toStringArray(dependentBeans);
	}

	/**
	 * <p>返回指定的bean依赖于所有的bean的名称，如果有的话。
	 * <hr>
	 * 
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			// 如果没有的话返回new String[0]而不是null
			return new String[0];
		}
		return dependenciesForBean.toArray(new String[dependenciesForBean.size()]);
	}

	/**
	 * <p>销毁所有单例。
	 * <hr>
	 * 
	 */
	public void destroySingletons() {
		if (logger.isInfoEnabled()) {
			logger.info("Destroying singletons in " + this);
		}
		// 单例目前销毁标志开始
		synchronized (this.singletonObjects) {
			this.singletonsCurrentlyInDestruction = true;
		}

		// 得到所有Bean名字。
		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		// 循环所有Bean名字的数组，一个一个地销毁Bean。
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		// containedBeanMap缓存清空,dependentBeanMap缓存清空，dependenciesForBeanMap缓存清空
		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * <p>销毁指定名称的一个单例Bean包括其依赖。
	 * <p>本方法内使用了{@link #removeSingleton(String)}从缓存中移除Bean，最后还调用了
	 *  {@link #destroyBean(String, DisposableBean)} 来销毁有依赖关系的Bean。
	 * <p>本方法同 {@link #destroyBean(String, DisposableBean)} 互相嵌套实现深度查找销毁Bean。
	 * <hr>
	 * 
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		// Remove a registered singleton of the given name, if any.
		// 从缓存中移除Bean。
		removeSingleton(beanName);

		// Destroy the corresponding DisposableBean instance.
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);
	}

	/**
	 * <p>销毁对指定Bean有各种依赖关系的Bean。
	 * <p>本方法主要是找到并移除指定Bean的依赖关系，从依赖关系中得到其它依赖
	 * Bean的名称，然后将它们传给{@link #destroySingleton(String)} 来销毁依赖Bean。
	 * <p>本方法同 {@link #destroySingleton(String)} 互相嵌套实现深度查找销毁Bean。
	 * <hr>
	 * 
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	protected void destroyBean(String beanName, DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		// 一、从dependentBeanMap依赖关系缓存中，移除关系，还得到所有依赖Bean名字的集合。
		Set<String> dependencies = this.dependentBeanMap.remove(beanName);
		if (dependencies != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			// 销毁依赖Bean。
			for (String dependentBeanName : dependencies) {
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		// 销毁bean实例
		if (bean != null) {
			try {
				bean.destroy();
			}
			catch (Throwable ex) {
				logger.error("Destroy method on bean with name '" + beanName + "' threw an exception", ex);
			}
		}

		// Trigger destruction of contained beans...
		// 二、从containedBeanMap依赖关系缓存中，移除关系，还得到所有依赖Bean名字的集合。
		Set<String> containedBeans = this.containedBeanMap.remove(beanName);
		if (containedBeans != null) {
			// 销毁依赖Bean。
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		// 从其它bean的依赖bean集合中移除要销毁的bean
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		// 最后从dependenciesForBeanMap缓存中移除要销毁的bean
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * <p>
	 * <hr>
	 * 
	 * Expose the singleton mutex to subclasses.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 */
	protected final Object getSingletonMutex() {
		return this.singletonObjects;
	}

}
