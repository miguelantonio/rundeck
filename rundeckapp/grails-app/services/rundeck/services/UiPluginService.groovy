package rundeck.services

import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.PluginRegistry
import com.dtolabs.rundeck.server.plugins.services.UIPluginProviderService

class UiPluginService {
    static boolean transactional = false
    def PluginRegistry rundeckPluginRegistry
    def PluginService pluginService
    def UIPluginProviderService uiPluginProviderService
    Map<String, Map<String, UIPlugin>> loadingCache = [:]
    List<String> loadedPlugins = []

    Map<String, UIPlugin> pluginsForPage(String path) {
        def reload = false
        def plugins = pluginService.listPlugins(UIPlugin, uiPluginProviderService)
        if (plugins.values()*.name.sort() != loadedPlugins) {
            loadedPlugins = plugins.values()*.name.sort()
            reload = true
        }
        if (!reload && loadingCache[path] != null) {
            return loadingCache[path]
        }

        def loaded = [:]
        plugins.each { String name, DescribedPlugin<UIPlugin> plugin ->
            UIPlugin inst = pluginService.getPlugin(plugin.name, uiPluginProviderService)
            if (inst.doesApply(path)) {
                loaded[plugin.name]= inst
            }
        }
        loadingCache[path] = loaded
        loaded
    }


    def getProfileFor(String service, String name) {
        def profile = [:]
        def reslist = resourcesForPlugin(service, name)
        if (reslist) {
            def testlist = ['png', 'gif'].inject([]) { list, ext ->
                list + ["${service}.${name}.icon.", 'icon.'].collect { prefix ->
                    prefix + ext
                }
            }
            profile['icon'] = testlist.find { reslist?.contains(it.toString()) }
        }
        profile['metadata'] = metadataForPlugin(service, name)
        profile
    }

    /**
     * List of resource names for the given plugin, or null
     * @param service
     * @param name
     * @return
     */
    def resourcesForPlugin(String service, String name) {
        rundeckPluginRegistry.getResourceLoader(service, name)?.listResources()
    }

    def metadataForPlugin(String service, String name) {
        rundeckPluginRegistry.getPluginMetadata(service, name)
    }
    /**
     * open input stream for the resource
     * @param service
     * @param name
     * @param path
     * @return
     */
    def openResourceForPlugin(String service, String name, String path) {
        try {
            return rundeckPluginRegistry.getResourceLoader(service, name)?.openResourceStreamFor(path)
        } catch (IOException | PluginException e) {
            return null
        }
    }
}
