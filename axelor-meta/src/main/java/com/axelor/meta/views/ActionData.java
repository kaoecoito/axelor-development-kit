package com.axelor.meta.views;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.data.ImportException;
import com.axelor.data.xml.XMLImporter;
import com.axelor.db.Model;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaStore;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@XmlType
public class ActionData extends Action {

	@XmlAttribute
	private String config;
	
	@XmlElement(name = "stream")
	private List<Stream> streams;
	
	public String getConfig() {
		return config;
	}
	
	public List<Stream> getStreams() {
		return streams;
	}

	private List<Model> doImport(XMLImporter importer, String fileName, Object data) {
		
		if (!(data instanceof String)) {
			log.debug("stream type not supported: " + data.getClass());
			return null;
		}
		
		log.info("action-data (import): " + fileName);
		
		final StringReader reader = new StringReader((String) data);
		final HashMultimap<String, Reader> mapping = HashMultimap.create();
		final List<Model> records = Lists.newArrayList();

		mapping.put(fileName, reader);
		
		importer.addListener(new XMLImporter.Listener() {
			@Override
			public void imported(Model bean) {
				log.info("action-data (record): {}(id={})",
						bean.getClass().getSimpleName(),
						bean.getId());
				records.add(bean);
			}
		});

		try {
			importer.process(mapping);
		} catch (ImportException e) {
			log.error("error:" + e);
			e.printStackTrace();
		}
		log.info("action-data (count): " + records.size());
		return records;
	}

	@Override
	public Object evaluate(ActionHandler handler) {
		log.info("action-data (config): " + config);
		XMLImporter importer = new XMLImporter(handler.getInjector(), config);
		Map<String, Object> result = Maps.newHashMap();
		
		importer.setContext(handler.getContext());

		int count = 0;
		for(Stream stream : getStreams()) {
			log.info("action-data (stream, provider): " + stream.file + ", " + stream.provider);
			Action action = MetaStore.getAction(stream.getProvider());
			if (action == null) {
				log.debug("No such action: " + stream.getProvider());
				continue;
			}
		
			List<Model> records = Lists.newArrayList();
			Object data = action.evaluate(handler);
			
			if (data instanceof Collection) {
				for(Object item : (Collection<?>) data) {
					if (item instanceof String) {
						log.info("action-data (xml stream)");
						List<Model> imported = doImport(importer, stream.file, item);
						if (imported != null) {
							records.addAll(imported);
						}
					}
				}
			} else {
				log.info("action-data (object stream)");
				List<Model> imported = doImport(importer, stream.file, data);
				if (imported != null) {
					records.addAll(imported);
				}
			}
			count += records.size();
			result.put(stream.name == null ? stream.file : stream.name, records);
		}
		log.info("action-data (total): " + count);
		return result;
	}

	@Override
	public Object wrap(ActionHandler handler) {
		Object records = evaluate(handler);
		return ImmutableMap.of("values", records);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(getClass()).add("name", getName()).toString();
	}
	
	@XmlType
	public static class Stream {
		
		@XmlAttribute
		private String file;
		
		@XmlAttribute
		private String provider;
		
		@XmlAttribute
		private String name;
		
		public String getFile() {
			return file;
		}
		
		public String getProvider() {
			return provider;
		}
		
		public String getName() {
			return name;
		}
		
		@Override
		public String toString() {
			return Objects.toStringHelper(getClass())
					.add("file", file)
					.add("provider", provider)
					.toString();
		}
	}
}
