package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.google.common.base.Joiner;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import hudson.util.XStream2;

import java.util.List;

/**
 * Created by aermakov on 13.11.15.
 */
public final class ConverterImpl extends XStream2.PassthruConverter<GitLabPushTrigger> {

    public static final String INCLUDE_BRANCHES_SPEC = "includeBranchesSpec";
    public static final String ALLOWED_BRANCHES_SPEC = "allowedBranchesSpec";
    public static final String ALLOWED_BRANCHES = "allowedBranches";

    public ConverterImpl(final XStream2 xstream) {
        super(xstream);

        xstream.registerLocalConverter(GitLabPushTrigger.class, INCLUDE_BRANCHES_SPEC, new Converter() {

            public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
                writer.setValue(String.valueOf(source));
            }

            public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
                if (INCLUDE_BRANCHES_SPEC.equalsIgnoreCase(reader.getNodeName())) {
                    return reader.getValue();
                }
                if (ALLOWED_BRANCHES_SPEC.equalsIgnoreCase(reader.getNodeName())) {
                    return reader.getValue();
                }
                if (ALLOWED_BRANCHES.equalsIgnoreCase(reader.getNodeName())) {
                    final Converter iconv = new CollectionConverter(xstream.getMapper(), List.class);
                    final List<?> list = (List<?>) iconv.unmarshal(reader, context);
                    return Joiner.on(',').join(list);
                }

                throw new AbstractReflectionConverter.UnknownFieldException(context.getRequiredType().getName(), reader.getNodeName());
            }

            public boolean canConvert(final Class type) {
                return List.class.isAssignableFrom(type) || String.class.isAssignableFrom(type);
            }
        });

        synchronized (xstream) {
            xstream.setMapper(new MapperWrapper(xstream.getMapperInjectionPoint()) {

                @Override
                public String realMember(final Class type, final String serialized) {
                    if (GitLabPushTrigger.class.equals(type)) {
                        if (ALLOWED_BRANCHES_SPEC.equalsIgnoreCase(serialized) || ALLOWED_BRANCHES.equalsIgnoreCase(serialized)) {
                            return INCLUDE_BRANCHES_SPEC;
                        }
                    }
                    return super.realMember(type, serialized);
                }

            });
        }
    }

    @Override
    protected void callback(final GitLabPushTrigger obj, final UnmarshallingContext context) {
        /* no-op */
    }

}
