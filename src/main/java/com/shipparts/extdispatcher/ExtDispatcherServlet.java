package com.shipparts.extdispatcher;

import com.shipparts.annotation.ExtController;
import com.shipparts.annotation.ExtRequestMapping;
import com.shipparts.utils.ClassUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义前端控制器
 */
public class ExtDispatcherServlet extends HttpServlet {

    //springmvc容器 key：类id value:对象
    private ConcurrentHashMap<String, Object> springmvcBean = new ConcurrentHashMap<>();

    //url-controller    key：请求地址  value：类
    private ConcurrentHashMap<String, Object> urlBeans = new ConcurrentHashMap<>();

    //url-method   key：请求地址  value：方法名称
    private ConcurrentHashMap<String, String> urlMethods = new ConcurrentHashMap<>();

    @Override
    public void init() throws ServletException {

        try {
            //1.获取当前包下的所有类
            List<Class> classes = ClassUtils.getClasses("com.shipparts.extcontroller");
            //2.扫包所有的类，注入springmvc容器  key：类名小写   value  对象
            findClassMVCAnnotation(classes);
            //3.url和方法关联起来
            handlerMapping();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //1.获取请求的url地址
        String requestURI = req.getRequestURI();
        if (StringUtils.isEmpty(requestURI)){
            return;
        }
        //2.map集合中获取控制器对象
        Object object = urlBeans.get(requestURI);
        if (object==null){
            resp.getWriter().println("not found 404 url");
            return;
        }
        //3.使用url从容器中获取方法
        String methodName = urlMethods.get(requestURI);
        if (methodName==null){
            resp.getWriter().println("not found method");
            return;
        }
        //4.反射机制调用方法、java反射机制获取方法返回结果
        String resultPage = (String) methodInvoke(object, methodName);
        //6.调用视图转换器，渲染试图
        ResourceViewResolver(resultPage,req,resp);
    }

    private Object methodInvoke(Object object,String methodName){
        try {
            Class<?> classInfo = object.getClass();
            Method method = classInfo.getMethod(methodName);
            Object result = method.invoke(object);
            return result;
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 视图解析
     * @param pageName
     * @param req
     * @param response
     */
    private void ResourceViewResolver(String pageName,HttpServletRequest req,HttpServletResponse response) throws ServletException, IOException {
        String prefix = "/";
        String suffix = ".jsp";
        req.getRequestDispatcher(prefix+pageName+suffix).forward(req,response);
    }

    /**
     * 扫包：注入springmvc容器
     *
     * @param classes
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public void findClassMVCAnnotation(List<Class> classes) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        for (Class<?> classInfo : classes) {
            //判断类上是否加controller注解
            ExtController extController = classInfo.getDeclaredAnnotation(ExtController.class);
            if (extController != null) {
                //类名小写
                String beanId = ClassUtils.toLowerCaseFirstOne(classInfo.getSimpleName());
                //实例化类对象
                Object object = ClassUtils.newInstance(classInfo);
                springmvcBean.put(beanId, object);
            }
        }
    }

    /**
     * url映射和方法关联
     */

    public void handlerMapping() {
        //1.获取springmvc容器的bean
        for (Map.Entry<String, Object> mvcBean : springmvcBean.entrySet()) {
            //1获取类对象
            Object object = mvcBean.getValue();
            //2.类上是否有url映射注解
            Class<?> classInfo = object.getClass();
            //3.判断类上是否有url映射注解
            ExtRequestMapping extRequestMappingClass = classInfo.getDeclaredAnnotation(ExtRequestMapping.class);
            String baseUrl = "";
            if (extRequestMappingClass != null) {
                //获取类上url映射地址
                baseUrl = extRequestMappingClass.value();
            }
            //4.判断方法是否有url映射地址
            Method[] methods = classInfo.getMethods();
            for (Method method : methods) {
                ExtRequestMapping extRequestMappingMethod = method.getDeclaredAnnotation(ExtRequestMapping.class);
                if (extRequestMappingMethod != null) {
                    String methodUrl = baseUrl + extRequestMappingMethod.value();
                    //url-controller    key：请求地址  value：类
                    urlBeans.put(methodUrl, object);
                    //url-method   key：请求地址  value：方法名称
                    urlMethods.put(methodUrl, method.getName());
                }
            }
        }
    }

}
