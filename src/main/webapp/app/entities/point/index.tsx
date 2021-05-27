import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import Point from './point';
import PointDetail from './point-detail';
import PointUpdate from './point-update';
import PointDeleteDialog from './point-delete-dialog';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/new`} component={PointUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id/edit`} component={PointUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={PointDetail} />
      <ErrorBoundaryRoute path={match.url} component={Point} />
    </Switch>
    <ErrorBoundaryRoute exact path={`${match.url}/:id/delete`} component={PointDeleteDialog} />
  </>
);

export default Routes;
