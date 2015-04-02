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

package org.springframework.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * <p>主要使用map作为别名的缓存，并对接口AliasRegistry进行实现。
 * <hr>
 * 
 * Simple implementation of the {@link AliasRegistry} interface.
 * Serves as base class for
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
 * implementations.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public class SimpleAliasRegistry implements AliasRegistry {

	/**
	 * <p>别名注册表，key为别名，value为名字。
	 * <hr>
	 * 
	 * Map from alias to canonical name.
	 **/
	private final Map<String, String> aliasMap = new ConcurrentHashMap<String, String>(16);


	public void registerAlias(String name, String alias) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		// 如果beanName与alias相同的话，不记录alias，并删除对应的alias。
		if (alias.equals(name)) {
			// 如果名字与别名相同，则删除原有的alias。
			this.aliasMap.remove(alias);
		}
		else {
			// 如果alias不允许被覆盖则抛出异常。
			if (!allowAliasOverriding()) { // 
				// 这里是不会进入if的，除非allowAliasOverriding被子类覆盖能返回false。
				// 能进入这里时，说明的名字与别名已经被注册过，是不允许被覆盖的。
				String registeredName = this.aliasMap.get(alias);
				if (registeredName != null && !registeredName.equals(name)) {
					// registeredName != null说明别名被注册过；
					// !registeredName.equals(name)说明已注册的名字与将要注册的名字不同。
					// 当两个条件都满足时，操作是将别名重新注册到另外一个名字上。
					// 这是覆盖，抛出异常阻止此操作。
					throw new IllegalStateException("Cannot register alias '" + alias + "' for name '" +
							name + "': It is already registered for name '" + registeredName + "'.");
				}
			}
			// 检测名字和别名是否能注册。
			// alias循环检查，当A->B存在时，若再次出现A->C->B时候则会抛出异常。
			checkForAliasCircle(name, alias);
			this.aliasMap.put(alias, name);
		}
	}

	/**
	 * Return whether alias overriding is allowed.
	 * 返回否是允许别名被覆盖。这里写死为true。
	 * Default is {@code true}.
	 */
	protected boolean allowAliasOverriding() {
		return true;
	}

	public void removeAlias(String alias) {
		String name = this.aliasMap.remove(alias);
		if (name == null) {
			throw new IllegalStateException("No alias '" + alias + "' registered");
		}
	}

	public boolean isAlias(String name) {
		return this.aliasMap.containsKey(name);
	}

	public String[] getAliases(String name) {
		List<String> result = new ArrayList<String>();
		synchronized (this.aliasMap) {
			retrieveAliases(name, result);
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * <p>循环注册表，查找同名字的别名，保存到List中。
	 * <hr>
	 * 
	 * Transitively retrieve all aliases for the given name.
	 * @param name the target name to find aliases for
	 * @param result the resulting aliases list
	 */
	private void retrieveAliases(String name, List<String> result) {
		for (Map.Entry<String, String> entry : this.aliasMap.entrySet()) {
			String registeredName = entry.getValue();
			if (registeredName.equals(name)) {
				String alias = entry.getKey();
				// 将别名保存到List中。
				result.add(alias);
				// 当别名作为名字而注册了另外的别名时，继续深度查找。
				retrieveAliases(alias, result);
			}
		}
	}

	/**
	 * Resolve all alias target names and aliases registered in this
	 * factory, applying the given StringValueResolver to them.
	 * 解析所有的名字和别名，并应用StringValueResolver到所有的名字和别名上。
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 * @param valueResolver the StringValueResolver to apply
	 */
	public void resolveAliases(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		synchronized (this.aliasMap) {
			Map<String, String> aliasCopy = new HashMap<String, String>(this.aliasMap);
			for (String alias : aliasCopy.keySet()) {
				// 根据别名获取名字。
				String registeredName = aliasCopy.get(alias);
				// 根据别名解析得到新解析别名。
				String resolvedAlias = valueResolver.resolveStringValue(alias);
				// 根据名字解析得到新解析名字。
				String resolvedName = valueResolver.resolveStringValue(registeredName);
				if (resolvedAlias.equals(resolvedName)) {
					// 如果新解析别名与新解析名字相同，则从别名注册表中移除。
					this.aliasMap.remove(alias);
				}
				else if (!resolvedAlias.equals(alias)) {
					// 如果新解析别名与原始别名不相同，则从别名注册表中查找对应的名字。
					String existingName = this.aliasMap.get(resolvedAlias);
					if (existingName != null && !existingName.equals(resolvedName)) {
						throw new IllegalStateException(
								"Cannot register resolved alias '" + resolvedAlias + "' (original: '" + alias +
								"') for name '" + resolvedName + "': It is already registered for name '" +
								registeredName + "'.");
					}
					checkForAliasCircle(resolvedName, resolvedAlias);
					this.aliasMap.remove(alias);
					this.aliasMap.put(resolvedAlias, resolvedName);
				}
				else if (!registeredName.equals(resolvedName)) {
					this.aliasMap.put(alias, resolvedName);
				}
			}
		}
	}

	/**
	 *<p>一般使用时是为某个名字注册一个别名，是可以注册多个别名的。
	 *也有可能这个“名字”实际上是另外一个真正名字的别名，但是最终要注册为真正名字的别名。
	 *这个方法就是为了找到真正名字的，所以使用时传递的参数为假“名字”，作为别名检索注册表。
	 *<hr>
	 * Determine the raw name, resolving aliases to canonical names.
	 * 确定原料的名称，解析别名规范名称。
	 * @param name the user-specified name
	 * @return the transformed name
	 */
	public String canonicalName(String name) {
		String canonicalName = name;
		// Handle aliasing...
		String resolvedName;
		do {
			// canonicalName作为别名，检索别名注册表。
			resolvedName = this.aliasMap.get(canonicalName);
			if (resolvedName != null) {
				// resolvedName是名字，但是也作为别名继续从注册表中检索。
				canonicalName = resolvedName;
			}
		}
		while (resolvedName != null); // resolvedName为null时，停止循环，canonicalName为最后的名字。
		return canonicalName;
	}

	/**
	 *<p>检测名字和别名是否能注册。检测使用到了{@link #canonicalName(String)}。
	 *<dl >
	 *<dt>Step1</dt><dd>注册名字为a-name，别名为a-value。    注册表结果为a-value ：a-name。    正常。</dd>
	 *<dt>Step2</dt><dd>注册名字为b-name，别名为b-value。    注册表结果为b-value ：b-name。    正常。</dd>
	 *<dt>Step3</dt><dd>注册名字为a-value，别名为c-value。   注册表结果为c-value：a-name。    正常。</dd>
	 *<dt>Step4</dt><dd>注册名字为a-value，别名为b-value。   注册表结果为b-value ：a-name。    正常，这一步将Setp2的结果修改。</dd>
	 *<dt>Step5</dt><dd>注册名字为a-value，别名为a-name。     注册表结果为a-name  ：a-name。    错误。</dd>
	 *</dl>
	 *<hr>
	 * Check whether the given name points back to given alias as an alias
	 * in the other direction, catching a circular reference upfront and
	 * throwing a corresponding IllegalStateException.
	 * @param name the candidate name
	 * @param alias the candidate alias
	 * @see #registerAlias
	 */
	protected void checkForAliasCircle(String name, String alias) {
		if (alias.equals(canonicalName(name))) {
			// 名字与别名是不能一样的。
			throw new IllegalStateException("Cannot register alias '" + alias +
					"' for name '" + name + "': Circular reference - '" +
					name + "' is a direct or indirect alias for '" + alias + "' already");
		}
	}

}
