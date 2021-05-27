import axios from 'axios';
import { ICrudGetAction, ICrudGetAllAction, ICrudPutAction, ICrudDeleteAction } from 'react-jhipster';

import { cleanEntity } from 'app/shared/util/entity-utils';
import { REQUEST, SUCCESS, FAILURE } from 'app/shared/reducers/action-type.util';

import { IPoint, defaultValue } from 'app/shared/model/point.model';

export const ACTION_TYPES = {
  FETCH_POINT_LIST: 'point/FETCH_POINT_LIST',
  FETCH_POINT: 'point/FETCH_POINT',
  CREATE_POINT: 'point/CREATE_POINT',
  UPDATE_POINT: 'point/UPDATE_POINT',
  PARTIAL_UPDATE_POINT: 'point/PARTIAL_UPDATE_POINT',
  DELETE_POINT: 'point/DELETE_POINT',
  RESET: 'point/RESET',
};

const initialState = {
  loading: false,
  errorMessage: null,
  entities: [] as ReadonlyArray<IPoint>,
  entity: defaultValue,
  updating: false,
  updateSuccess: false,
};

export type PointState = Readonly<typeof initialState>;

// Reducer

export default (state: PointState = initialState, action): PointState => {
  switch (action.type) {
    case REQUEST(ACTION_TYPES.FETCH_POINT_LIST):
    case REQUEST(ACTION_TYPES.FETCH_POINT):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        loading: true,
      };
    case REQUEST(ACTION_TYPES.CREATE_POINT):
    case REQUEST(ACTION_TYPES.UPDATE_POINT):
    case REQUEST(ACTION_TYPES.DELETE_POINT):
    case REQUEST(ACTION_TYPES.PARTIAL_UPDATE_POINT):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        updating: true,
      };
    case FAILURE(ACTION_TYPES.FETCH_POINT_LIST):
    case FAILURE(ACTION_TYPES.FETCH_POINT):
    case FAILURE(ACTION_TYPES.CREATE_POINT):
    case FAILURE(ACTION_TYPES.UPDATE_POINT):
    case FAILURE(ACTION_TYPES.PARTIAL_UPDATE_POINT):
    case FAILURE(ACTION_TYPES.DELETE_POINT):
      return {
        ...state,
        loading: false,
        updating: false,
        updateSuccess: false,
        errorMessage: action.payload,
      };
    case SUCCESS(ACTION_TYPES.FETCH_POINT_LIST):
      return {
        ...state,
        loading: false,
        entities: action.payload.data,
      };
    case SUCCESS(ACTION_TYPES.FETCH_POINT):
      return {
        ...state,
        loading: false,
        entity: action.payload.data,
      };
    case SUCCESS(ACTION_TYPES.CREATE_POINT):
    case SUCCESS(ACTION_TYPES.UPDATE_POINT):
    case SUCCESS(ACTION_TYPES.PARTIAL_UPDATE_POINT):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        entity: action.payload.data,
      };
    case SUCCESS(ACTION_TYPES.DELETE_POINT):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        entity: {},
      };
    case ACTION_TYPES.RESET:
      return {
        ...initialState,
      };
    default:
      return state;
  }
};

const apiUrl = 'api/points';

// Actions

export const getEntities: ICrudGetAllAction<IPoint> = (page, size, sort) => ({
  type: ACTION_TYPES.FETCH_POINT_LIST,
  payload: axios.get<IPoint>(`${apiUrl}?cacheBuster=${new Date().getTime()}`),
});

export const getEntity: ICrudGetAction<IPoint> = id => {
  const requestUrl = `${apiUrl}/${id}`;
  return {
    type: ACTION_TYPES.FETCH_POINT,
    payload: axios.get<IPoint>(requestUrl),
  };
};

export const createEntity: ICrudPutAction<IPoint> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.CREATE_POINT,
    payload: axios.post(apiUrl, cleanEntity(entity)),
  });
  dispatch(getEntities());
  return result;
};

export const updateEntity: ICrudPutAction<IPoint> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.UPDATE_POINT,
    payload: axios.put(`${apiUrl}/${entity.id}`, cleanEntity(entity)),
  });
  return result;
};

export const partialUpdate: ICrudPutAction<IPoint> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.PARTIAL_UPDATE_POINT,
    payload: axios.patch(`${apiUrl}/${entity.id}`, cleanEntity(entity)),
  });
  return result;
};

export const deleteEntity: ICrudDeleteAction<IPoint> = id => async dispatch => {
  const requestUrl = `${apiUrl}/${id}`;
  const result = await dispatch({
    type: ACTION_TYPES.DELETE_POINT,
    payload: axios.delete(requestUrl),
  });
  dispatch(getEntities());
  return result;
};

export const reset = () => ({
  type: ACTION_TYPES.RESET,
});
