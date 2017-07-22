package org.walkerljl.toolkit.ioc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.walkerljl.toolkit.ioc.annotation.Resource;
import org.walkerljl.toolkit.lang.ArraysUtils;
import org.walkerljl.toolkit.lang.ListUtils;
import org.walkerljl.toolkit.logging.Logger;
import org.walkerljl.toolkit.logging.LoggerFactory;
import org.walkerljl.toolkit.scanner.ClassScannerUtils;

/**
 * 
 * IOC容器
 *
 * @author lijunlin
 */
public class IocFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(IocFactory.class);

	/** 包名 */
	private String packageName;
	/** Bean 工厂 */
	private BeanFactory beanFactory;

	/**
	 * 构造函数
	 * 
	 * @param packageName
	 */
	public IocFactory(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * 初始化
	 */
	public void init() {
		try {
			this.beanFactory = new BeanFactory(this.packageName);
			this.beanFactory.init();
			Map<Class<?>, Object> beanMap = beanFactory.getBeanMap();
			for (Map.Entry<Class<?>, Object> beanEntry : beanMap.entrySet()) {
				Class<?> beanClass = beanEntry.getKey();
				Field[] beanFields = beanClass.getDeclaredFields();
				if (ArraysUtils.isEmpty(beanFields)) {
					continue;
				}
				for (Field beanField : beanFields) {
					if (!beanField.isAnnotationPresent(Resource.class)) {
						continue;
					}
					Class<?> interfaceClass = beanField.getType();
					Class<?> implementClass = findImplementClass(interfaceClass);
					if (implementClass == null) {
						continue;
					}
					Object implementInstance = beanMap.get(implementClass);
					if (implementInstance != null) {
						beanField.setAccessible(true);
						Object beanInstance = beanEntry.getValue();
						beanField.set(beanInstance, implementInstance);
					} else {
						String errMsg = String.format("Fail to register dependency, class : %s, filed : %s", beanClass.getName(), interfaceClass.getName());
						LOGGER.error(errMsg);
						throw new IocException(errMsg);
					}
				}
			}
		} catch (Exception e) {
			String errMsg = "Fail to init IOC Factory";
			LOGGER.error(errMsg);
			throw new IocException(errMsg, e);
		}
	}

	/**
	 * 查找实现类
	 * 
	 * @param interfaceClass
	 * @return
	 */
	private Class<?> findImplementClass(Class<?> interfaceClass) {
		Class<?> implementClass = interfaceClass;
		if (interfaceClass.isAnnotationPresent(Resource.class)) {
			implementClass = interfaceClass.getAnnotation(Resource.class).impl();
		} else {
			List<Class<?>> implementClassList = ClassScannerUtils.getClassListBySuper(packageName, interfaceClass);
			if (ListUtils.isNotEmpty(implementClassList)) {
				implementClass = implementClassList.get(0);
			}
		}
		return implementClass;
	}

	/**
	 * 获取Bean实例
	 * 
	 * @param clazz
	 * @return
	 */
	public Object getBean(Class<?> clazz) {
		if (clazz == null) {
			return null;
		}
		if (clazz.isInterface()) {
			Class<?> targetClass = findImplementClass(clazz);
			return targetClass == null ? null : getBean(targetClass);
		} else {
			return beanFactory.getBean(clazz);
		}
	}
	
	/**
	 * 设置Bean实例
	 * 
	 * @param clazz
	 * @param object
	 */
	public void setBean(Class<?> clazz, Object object) {
		beanFactory.setBean(clazz, object);
	}
}