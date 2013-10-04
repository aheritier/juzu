/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.impl.plugin.template;

import juzu.impl.common.Name;
import juzu.impl.plugin.PluginDescriptor;
import juzu.impl.plugin.PluginContext;
import juzu.impl.plugin.application.ApplicationPlugin;
import juzu.impl.plugin.template.metadata.TemplateDescriptor;
import juzu.impl.template.spi.TemplateStub;
import juzu.impl.plugin.template.metadata.TemplatesDescriptor;
import juzu.impl.common.Path;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class TemplatePlugin extends ApplicationPlugin {

  /** . */
  private TemplatesDescriptor descriptor;

  /** . */
  private final ConcurrentHashMap<Path, TemplateStub> stubs;

  /** . */
  private PluginContext context;

  public TemplatePlugin() {
    super("template");

    //
    this.stubs = new ConcurrentHashMap<Path, TemplateStub>();
  }

  public TemplatesDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public PluginDescriptor init(PluginContext context) throws Exception {
    this.context = context;
    this.descriptor = new TemplatesDescriptor(application, context.getClassLoader(), context.getConfig());
    return descriptor;
  }

  public TemplateStub resolveTemplateStub(String path) {
    return resolveTemplateStub(juzu.impl.common.Path.parse(path));
  }

  public TemplateStub resolveTemplateStub(juzu.impl.common.Path path) {
    TemplateStub stub = stubs.get(path);
    if (stub == null) {

      //
      Path.Absolute resolved = descriptor.getPackage().resolve(path);

      //
      TemplateDescriptor desc;
      try {
        Class<?> clazz = context.getClassLoader().loadClass(resolved.getName().toString());
        Field f = clazz.getField("DESCRIPTOR");
        desc = (TemplateDescriptor)f.get(null);
      }
      catch (Exception e) {
        throw new UnsupportedOperationException("Handle me gracefully", e);
      }

      //
      stub = desc.getStub();

      //
      TemplateStub phantom = stubs.putIfAbsent(path, stub);
      if (phantom != null) {
        stub = phantom;
      } else {
        stub.init();
      }
    }

    //
    return stub;
  }
}
