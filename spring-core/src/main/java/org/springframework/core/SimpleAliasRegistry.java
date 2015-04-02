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
 * <p>��Ҫʹ��map��Ϊ�����Ļ��棬���Խӿ�AliasRegistry����ʵ�֡�
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
	 * <p>����ע���keyΪ������valueΪ���֡�
	 * <hr>
	 * 
	 * Map from alias to canonical name.
	 **/
	private final Map<String, String> aliasMap = new ConcurrentHashMap<String, String>(16);


	public void registerAlias(String name, String alias) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		// ���beanName��alias��ͬ�Ļ�������¼alias����ɾ����Ӧ��alias��
		if (alias.equals(name)) {
			// ��������������ͬ����ɾ��ԭ�е�alias��
			this.aliasMap.remove(alias);
		}
		else {
			// ���alias�������������׳��쳣��
			if (!allowAliasOverriding()) { // 
				// �����ǲ������if�ģ�����allowAliasOverriding�����า���ܷ���false��
				// �ܽ�������ʱ��˵��������������Ѿ���ע������ǲ��������ǵġ�
				String registeredName = this.aliasMap.get(alias);
				if (registeredName != null && !registeredName.equals(name)) {
					// registeredName != null˵��������ע�����
					// !registeredName.equals(name)˵����ע��������뽫Ҫע������ֲ�ͬ��
					// ����������������ʱ�������ǽ���������ע�ᵽ����һ�������ϡ�
					// ���Ǹ��ǣ��׳��쳣��ֹ�˲�����
					throw new IllegalStateException("Cannot register alias '" + alias + "' for name '" +
							name + "': It is already registered for name '" + registeredName + "'.");
				}
			}
			// ������ֺͱ����Ƿ���ע�ᡣ
			// aliasѭ����飬��A->B����ʱ�����ٴγ���A->C->Bʱ������׳��쳣��
			checkForAliasCircle(name, alias);
			this.aliasMap.put(alias, name);
		}
	}

	/**
	 * Return whether alias overriding is allowed.
	 * ���ط���������������ǡ�����д��Ϊtrue��
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
	 * <p>ѭ��ע�������ͬ���ֵı��������浽List�С�
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
				// ���������浽List�С�
				result.add(alias);
				// ��������Ϊ���ֶ�ע��������ı���ʱ��������Ȳ��ҡ�
				retrieveAliases(alias, result);
			}
		}
	}

	/**
	 * Resolve all alias target names and aliases registered in this
	 * factory, applying the given StringValueResolver to them.
	 * �������е����ֺͱ�������Ӧ��StringValueResolver�����е����ֺͱ����ϡ�
	 * <p>The value resolver may for example resolve placeholders
	 * in target bean names and even in alias names.
	 * @param valueResolver the StringValueResolver to apply
	 */
	public void resolveAliases(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		synchronized (this.aliasMap) {
			Map<String, String> aliasCopy = new HashMap<String, String>(this.aliasMap);
			for (String alias : aliasCopy.keySet()) {
				// ���ݱ�����ȡ���֡�
				String registeredName = aliasCopy.get(alias);
				// ���ݱ��������õ��½���������
				String resolvedAlias = valueResolver.resolveStringValue(alias);
				// �������ֽ����õ��½������֡�
				String resolvedName = valueResolver.resolveStringValue(registeredName);
				if (resolvedAlias.equals(resolvedName)) {
					// ����½����������½���������ͬ����ӱ���ע������Ƴ���
					this.aliasMap.remove(alias);
				}
				else if (!resolvedAlias.equals(alias)) {
					// ����½���������ԭʼ��������ͬ����ӱ���ע����в��Ҷ�Ӧ�����֡�
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
	 *<p>һ��ʹ��ʱ��Ϊĳ������ע��һ���������ǿ���ע���������ġ�
	 *Ҳ�п�����������֡�ʵ����������һ���������ֵı�������������Ҫע��Ϊ�������ֵı�����
	 *�����������Ϊ���ҵ��������ֵģ�����ʹ��ʱ���ݵĲ���Ϊ�١����֡�����Ϊ��������ע���
	 *<hr>
	 * Determine the raw name, resolving aliases to canonical names.
	 * ȷ��ԭ�ϵ����ƣ����������淶���ơ�
	 * @param name the user-specified name
	 * @return the transformed name
	 */
	public String canonicalName(String name) {
		String canonicalName = name;
		// Handle aliasing...
		String resolvedName;
		do {
			// canonicalName��Ϊ��������������ע���
			resolvedName = this.aliasMap.get(canonicalName);
			if (resolvedName != null) {
				// resolvedName�����֣�����Ҳ��Ϊ����������ע����м�����
				canonicalName = resolvedName;
			}
		}
		while (resolvedName != null); // resolvedNameΪnullʱ��ֹͣѭ����canonicalNameΪ�������֡�
		return canonicalName;
	}

	/**
	 *<p>������ֺͱ����Ƿ���ע�ᡣ���ʹ�õ���{@link #canonicalName(String)}��
	 *<dl >
	 *<dt>Step1</dt><dd>ע������Ϊa-name������Ϊa-value��    ע�����Ϊa-value ��a-name��    ������</dd>
	 *<dt>Step2</dt><dd>ע������Ϊb-name������Ϊb-value��    ע�����Ϊb-value ��b-name��    ������</dd>
	 *<dt>Step3</dt><dd>ע������Ϊa-value������Ϊc-value��   ע�����Ϊc-value��a-name��    ������</dd>
	 *<dt>Step4</dt><dd>ע������Ϊa-value������Ϊb-value��   ע�����Ϊb-value ��a-name��    ��������һ����Setp2�Ľ���޸ġ�</dd>
	 *<dt>Step5</dt><dd>ע������Ϊa-value������Ϊa-name��     ע�����Ϊa-name  ��a-name��    ����</dd>
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
			// ����������ǲ���һ���ġ�
			throw new IllegalStateException("Cannot register alias '" + alias +
					"' for name '" + name + "': Circular reference - '" +
					name + "' is a direct or indirect alias for '" + alias + "' already");
		}
	}

}
